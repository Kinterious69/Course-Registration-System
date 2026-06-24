package com.university.registration.service;

import com.university.registration.concurrent.EnrollmentQueue;
import com.university.registration.concurrent.EnrollmentQueue.EnrollmentRequest;
import com.university.registration.exception.*;
import com.university.registration.model.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * RegistrationService — the core business logic layer.
 *
 * Wires together:
 *   - StudentRegistry<Student>  (generic ADT)
 *   - StudentRegistry<Course>   (generic ADT reused for courses)
 *   - EnrollmentQueue           (concurrency)
 *   - Python analytics module   (inter-language REST call)
 *
 * Demonstrates:
 *   - Exception propagation and handling
 *   - Generics in practice
 *   - Lambda expressions (stream operations)
 *   - Inter-language communication via Java HttpClient
 *   - Reusable service component
 */
public class RegistrationService {

    private static final Logger log = Logger.getLogger(RegistrationService.class.getName());

    // ── Python module URL ─────────────────────────────────────────────
    private static final String PYTHON_URL = "http://localhost:8000/recommend";

    // ── Registries ────────────────────────────────────────────────────
    private final StudentRegistry<Student> studentRegistry;
    private final StudentRegistry<Course>  courseRegistry;

    // ── Enrollment records ────────────────────────────────────────────
    private final List<Enrollment> enrollments = Collections.synchronizedList(new ArrayList<>());

    // ── Concurrency ───────────────────────────────────────────────────
    private final EnrollmentQueue enrollmentQueue;

    // ── HTTP client for calling Python ────────────────────────────────
    private final HttpClient httpClient;

    // ── Constructor ───────────────────────────────────────────────────

    public RegistrationService() {
        this.studentRegistry = new StudentRegistry<>("Students");
        this.courseRegistry  = new StudentRegistry<>("Courses");
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

        // Start the enrollment queue with 3 worker threads
        this.enrollmentQueue = new EnrollmentQueue(50, 3, this::processRequest);

        log.info("RegistrationService initialised.");
    }

    // ── Student management ────────────────────────────────────────────

    public void addStudent(Student student) {
        studentRegistry.register(student);
        log.info("Registered student: " + student.getStudentId());
    }

    public Student getStudent(String studentId) throws StudentNotFoundException {
        return studentRegistry.findById(studentId)
            .orElseThrow(() -> new StudentNotFoundException(studentId));
    }

    // ── Course management ─────────────────────────────────────────────

    public void addCourse(Course course) {
        courseRegistry.register(course);
        log.info("Added course: " + course.getCourseId());
    }

    public Course getCourse(String courseId) throws StudentNotFoundException {
        return courseRegistry.findById(courseId)
            .orElseThrow(() -> new StudentNotFoundException(courseId));
    }

    // ── Synchronous enrollment (used directly in Main / tests) ────────

    /**
     * Enrolls a student in a course immediately (synchronous path).
     *
     * Validates:
     *   1. Student exists
     *   2. Course exists
     *   3. Prerequisites met
     *   4. Seat available
     *
     * On success, calls Python analytics module for recommendations.
     *
     * @throws StudentNotFoundException    if student or course ID not found
     * @throws PrerequisiteNotMetException if student is missing prerequisites
     * @throws CourseFullException         if the course has no seats left
     */
    public Enrollment enroll(String studentId, String courseId)
            throws RegistrationException {

        // Step 1 — validate student exists
        Student student = studentRegistry.findById(studentId)
            .orElseThrow(() -> new StudentNotFoundException(studentId));

        // Step 2 — validate course exists
        Course course = courseRegistry.findById(courseId)
            .orElseThrow(() -> new StudentNotFoundException(courseId));

        // Step 3 — check prerequisites
        List<String> missing = student.findMissingPrerequisites(
            course.getPrerequisiteCourseIds()
        );
        if (!missing.isEmpty()) {
            throw new PrerequisiteNotMetException(courseId, missing);
        }

        // Step 4 — attempt to reserve a seat (thread-safe via synchronized)
        boolean reserved = course.reserveSeat();
        if (!reserved) {
            throw new CourseFullException(courseId, course.getMaxCapacity());
        }

        // Step 5 — create and store enrollment record
        Enrollment enrollment = new Enrollment(studentId, courseId);
        enrollments.add(enrollment);
        log.info("Enrolled: " + studentId + " → " + courseId);

        // Step 6 — call Python analytics (non-blocking, best-effort)
        callPythonRecommender(studentId, courseId, student.getCompletedCourseIds());

        return enrollment;
    }

    // ── Drop a course ─────────────────────────────────────────────────

    /**
     * Drops an active enrollment for the given student and course.
     *
     * @throws RegistrationException if no active enrollment is found
     */
    public void drop(String studentId, String courseId) throws RegistrationException {

        // Validate both exist
        if (!studentRegistry.contains(studentId))
            throw new StudentNotFoundException(studentId);

        // Find the active enrollment
        Enrollment active = enrollments.stream()
            .filter(e -> e.getStudentId().equals(studentId)
                      && e.getCourseId().equals(courseId)
                      && e.isActive())
            .findFirst()
            .orElseThrow(() -> new RegistrationException(
                "No active enrollment found for student " + studentId
                    + " in course " + courseId,
                "ENROLLMENT_NOT_FOUND"
            ));

        // Release the seat back to the course
        courseRegistry.findById(courseId).ifPresent(Course::releaseSeat);

        active.drop();
        log.info("Dropped: " + studentId + " → " + courseId);
    }

    // ── Asynchronous enrollment (via queue) ───────────────────────────

    /**
     * Submits an enrollment request to the concurrent queue.
     * Returns immediately — processing happens on a worker thread.
     * Used to simulate many students enrolling simultaneously.
     */
    public boolean submitAsync(String studentId, String courseId) {
        return enrollmentQueue.submit(new EnrollmentRequest(studentId, courseId));
    }

    /**
     * Processes a request taken from the EnrollmentQueue.
     * This runs on a worker thread — must be thread-safe.
     */
    private void processRequest(EnrollmentRequest request) {
        try {
            Enrollment e = enroll(request.getStudentId(), request.getCourseId());
            log.info("[ASYNC] Success: " + e.getEnrollmentId());
        } catch (CourseFullException ex) {
            log.warning("[ASYNC] Course full: " + ex);
        } catch (PrerequisiteNotMetException ex) {
            log.warning("[ASYNC] Prereqs not met: " + ex);
        } catch (RegistrationException ex) {
            log.severe("[ASYNC] Error: " + ex);
        }
    }

    // ── Queries ───────────────────────────────────────────────────────

    /** Returns all active enrollments for a student. */
    public List<Enrollment> getActiveEnrollments(String studentId) {
        return enrollments.stream()
            .filter(e -> e.getStudentId().equals(studentId) && e.isActive())
            .collect(Collectors.toList());
    }

    /** Returns all courses that still have available seats. */
    public List<Course> getAvailableCourses() {
        return courseRegistry.findWhere(Course::hasAvailableSeats);
    }

    /** Returns total enrollment count across all courses. */
    public long getTotalEnrollments() {
        return enrollments.stream().filter(Enrollment::isActive).count();
    }

    public void shutdown() {
        enrollmentQueue.shutdown();
    }

    // ── Python inter-language call ────────────────────────────────────

    /**
     * Calls the Python FastAPI module with the student's completed courses.
     * Python returns a list of recommended course IDs.
     *
     * Inter-language communication via HTTP/REST (JSON payload).
     * Errors are caught and logged — a Python failure never crashes Java.
     */
    private void callPythonRecommender(String studentId, String enrolledCourseId,
                                        List<String> completedCourses) {
        try {
            // Build a simple JSON payload manually (no library needed)
            String completedJson = completedCourses.stream()
                .map(c -> "\"" + c + "\"")
                .collect(Collectors.joining(",", "[", "]"));

            String json = String.format(
                "{\"student_id\":\"%s\",\"enrolled_course\":\"%s\",\"completed_courses\":%s}",
                studentId, enrolledCourseId, completedJson
            );

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(PYTHON_URL))
                .timeout(Duration.ofSeconds(3))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

            HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            log.info("[Python] Recommendations for " + studentId
                + ": " + response.body());

        } catch (IOException | InterruptedException e) {
            // Python module is offline — log it, don't crash Java
            log.warning("[Python] Analytics module unavailable: " + e.getMessage());
        }
    }
}
