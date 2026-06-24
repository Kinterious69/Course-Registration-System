package com.university.registration.service;

import com.university.registration.exception.*;
import com.university.registration.model.*;
import com.university.registration.model.Course.Department;
import com.university.registration.model.Student.AcademicLevel;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Integration + stress tests for RegistrationService.
 *
 * Tests: enrollment happy path, all exception types, drop logic,
 * concurrent enrollment stress test (race condition detection),
 * async queue processing.
 *
 * Run with: java -cp out com.university.registration.service.RegistrationServiceTest
 */
public class RegistrationServiceTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== RegistrationServiceTest ===\n");

        test_enroll_happyPath();
        test_enroll_studentNotFound();
        test_enroll_prerequisiteNotMet();
        test_enroll_courseFull();
        test_drop_activeEnrollment();
        test_drop_noActiveEnrollment_throws();
        test_getAvailableCourses_filtersFullCourses();
        test_enroll_concurrent_stressTest();
        test_async_queue_processes_requests();

        System.out.println("\n--- Results ---");
        System.out.println("Passed: " + passed);
        System.out.println("Failed: " + failed);
        if (failed > 0) System.exit(1);
    }

    // ── Helper: fresh service with standard test data ──────────────────

    static RegistrationService freshService() throws RegistrationException {
        RegistrationService svc = new RegistrationService();
        svc.addCourse(new Course("CPS101", "Intro", Department.COMPUTER_SCIENCE, 3, 30));
        svc.addCourse(new Course("CPS314", "PL", Department.COMPUTER_SCIENCE, 3, 2,
            List.of("CPS101", "CPS201")));
        svc.addStudent(new Student("S001", "Alice", "alice@uni.edu",
            AcademicLevel.SOPHOMORE, List.of("CPS101", "CPS201")));
        svc.addStudent(new Student("S002", "Bob", "bob@uni.edu",
            AcademicLevel.FRESHMAN));
        return svc;
    }

    // ── Enroll: happy path ────────────────────────────────────────────

    static void test_enroll_happyPath() {
        try {
            RegistrationService svc = freshService();
            Enrollment e = svc.enroll("S001", "CPS314");
            assert_true(e != null, "Enrollment should not be null");
            assert_equals("S001", e.getStudentId(), "studentId on enrollment");
            assert_equals("CPS314", e.getCourseId(), "courseId on enrollment");
            assert_true(e.isActive(), "Enrollment should be ACTIVE");
            assert_equals(1L, svc.getTotalEnrollments(), "Total enrollments should be 1");
            pass("test_enroll_happyPath");
        } catch (Exception e) { fail("test_enroll_happyPath", e); }
    }

    // ── Enroll: exceptions ────────────────────────────────────────────

    static void test_enroll_studentNotFound() {
        try {
            RegistrationService svc = freshService();
            svc.enroll("S999", "CPS101");   // unknown student
            fail("test_enroll_studentNotFound", "Expected StudentNotFoundException");
        } catch (StudentNotFoundException e) {
            assert_equals("STUDENT_NOT_FOUND", e.getErrorCode(), "error code");
            pass("test_enroll_studentNotFound");
        } catch (Exception e) { fail("test_enroll_studentNotFound", e); }
    }

    static void test_enroll_prerequisiteNotMet() {
        try {
            RegistrationService svc = freshService();
            svc.enroll("S002", "CPS314");   // Bob has no prereqs
            fail("test_enroll_prerequisiteNotMet", "Expected PrerequisiteNotMetException");
        } catch (PrerequisiteNotMetException e) {
            assert_true(e.getMissingPrerequisites().contains("CPS101"),
                "CPS101 should be listed as missing");
            assert_equals("PREREQUISITE_NOT_MET", e.getErrorCode(), "error code");
            pass("test_enroll_prerequisiteNotMet");
        } catch (Exception e) { fail("test_enroll_prerequisiteNotMet", e); }
    }

    static void test_enroll_courseFull() {
        try {
            RegistrationService svc = freshService();
            // CPS314 has capacity 2 — add a second student with prereqs
            svc.addStudent(new Student("S003", "Carol", "carol@uni.edu",
                AcademicLevel.SOPHOMORE, List.of("CPS101", "CPS201")));
            svc.addStudent(new Student("S004", "Dave", "dave@uni.edu",
                AcademicLevel.SOPHOMORE, List.of("CPS101", "CPS201")));

            svc.enroll("S001", "CPS314");   // seat 1
            svc.enroll("S003", "CPS314");   // seat 2 — now full
            svc.enroll("S004", "CPS314");   // should throw

            fail("test_enroll_courseFull", "Expected CourseFullException");
        } catch (CourseFullException e) {
            assert_equals("COURSE_FULL", e.getErrorCode(), "error code");
            assert_equals("CPS314", e.getCourseId(), "courseId in exception");
            pass("test_enroll_courseFull");
        } catch (Exception e) { fail("test_enroll_courseFull", e); }
    }

    // ── Drop ──────────────────────────────────────────────────────────

    static void test_drop_activeEnrollment() {
        try {
            RegistrationService svc = freshService();
            svc.enroll("S001", "CPS314");
            assert_equals(1L, svc.getTotalEnrollments(), "1 enrollment before drop");
            svc.drop("S001", "CPS314");
            assert_equals(0L, svc.getTotalEnrollments(), "0 enrollments after drop");
            // Seat should be released — another student can now enroll
            svc.addStudent(new Student("S003", "Carol", "carol@uni.edu",
                AcademicLevel.SOPHOMORE, List.of("CPS101", "CPS201")));
            Enrollment e2 = svc.enroll("S003", "CPS314");
            assert_true(e2.isActive(), "Should be able to enroll after seat released");
            pass("test_drop_activeEnrollment");
        } catch (Exception e) { fail("test_drop_activeEnrollment", e); }
    }

    static void test_drop_noActiveEnrollment_throws() {
        try {
            RegistrationService svc = freshService();
            svc.drop("S001", "CPS314");   // never enrolled
            fail("test_drop_noActiveEnrollment_throws", "Expected RegistrationException");
        } catch (RegistrationException e) {
            assert_equals("ENROLLMENT_NOT_FOUND", e.getErrorCode(), "error code");
            pass("test_drop_noActiveEnrollment_throws");
        } catch (Exception e) { fail("test_drop_noActiveEnrollment_throws", e); }
    }

    // ── Queries ───────────────────────────────────────────────────────

    static void test_getAvailableCourses_filtersFullCourses() {
        try {
            RegistrationService svc = freshService();
            svc.addStudent(new Student("S003", "Carol", "carol@uni.edu",
                AcademicLevel.SOPHOMORE, List.of("CPS101", "CPS201")));
            svc.enroll("S001", "CPS314");
            svc.enroll("S003", "CPS314");   // CPS314 now full (capacity 2)

            List<Course> available = svc.getAvailableCourses();
            boolean cps314Present = available.stream()
                .anyMatch(c -> c.getCourseId().equals("CPS314"));
            assert_true(!cps314Present, "Full course CPS314 should not appear in available list");
            boolean cps101Present = available.stream()
                .anyMatch(c -> c.getCourseId().equals("CPS101"));
            assert_true(cps101Present, "CPS101 with seats should appear in available list");
            pass("test_getAvailableCourses_filtersFullCourses");
        } catch (Exception e) { fail("test_getAvailableCourses_filtersFullCourses", e); }
    }

    // ── Concurrency stress test ───────────────────────────────────────

    /**
     * Stress test: 20 threads all try to enroll in a course with 5 seats.
     * Exactly 5 should succeed, 15 should get CourseFullException.
     * Any other result = race condition bug.
     */
    static void test_enroll_concurrent_stressTest() throws InterruptedException {
        try {
            int capacity  = 5;
            int attempts  = 20;
            RegistrationService svc = new RegistrationService();
            svc.addCourse(new Course("CPS999", "Stress", Department.COMPUTER_SCIENCE, 3, capacity));

            // Create 'attempts' students all with prereqs satisfied
            for (int i = 1; i <= attempts; i++) {
                svc.addStudent(new Student(
                    "T" + i, "Student " + i, "s" + i + "@uni.edu", AcademicLevel.SOPHOMORE
                ));
            }

            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger fullCount    = new AtomicInteger(0);
            CountDownLatch ready  = new CountDownLatch(attempts);
            CountDownLatch start  = new CountDownLatch(1);
            CountDownLatch done   = new CountDownLatch(attempts);

            for (int i = 1; i <= attempts; i++) {
                final String sid = "T" + i;
                new Thread(() -> {
                    ready.countDown();
                    try { start.await(); } catch (InterruptedException ex) { return; }
                    try {
                        svc.enroll(sid, "CPS999");
                        successCount.incrementAndGet();
                    } catch (CourseFullException ex) {
                        fullCount.incrementAndGet();
                    } catch (RegistrationException ex) {
                        // unexpected
                    } finally {
                        done.countDown();
                    }
                }).start();
            }

            ready.await();           // all threads ready
            long startMs = System.currentTimeMillis();
            start.countDown();       // fire all threads at once
            done.await();            // wait for all to finish
            long duration = System.currentTimeMillis() - startMs;

            System.out.println("  [STRESS] " + attempts + " concurrent enrollments for "
                + capacity + " seats — " + duration + "ms");
            System.out.println("  [STRESS] Successes: " + successCount.get()
                + " | Full rejections: " + fullCount.get());

            assert_equals(capacity, successCount.get(),
                "Exactly " + capacity + " enrollments should succeed");
            assert_equals(attempts - capacity, fullCount.get(),
                "Remaining " + (attempts - capacity) + " should get CourseFullException");
            assert_equals(successCount.get() + fullCount.get(), attempts,
                "Total outcomes must equal total attempts");

            pass("test_enroll_concurrent_stressTest");
        } catch (AssertionError e) { fail("test_enroll_concurrent_stressTest", e); }
    }

    // ── Async queue ───────────────────────────────────────────────────

    static void test_async_queue_processes_requests() throws InterruptedException {
        try {
            RegistrationService svc = new RegistrationService();
            svc.addCourse(new Course("CPS101", "Intro", Department.COMPUTER_SCIENCE, 3, 10));
            for (int i = 1; i <= 5; i++) {
                svc.addStudent(new Student("Q" + i, "Student " + i,
                    "q" + i + "@uni.edu", AcademicLevel.FRESHMAN));
            }

            for (int i = 1; i <= 5; i++) {
                boolean queued = svc.submitAsync("Q" + i, "CPS101");
                assert_true(queued, "Request Q" + i + " should be accepted by queue");
            }

            Thread.sleep(1000);   // allow workers to drain

            assert_true(svc.getTotalEnrollments() > 0,
                "At least some async enrollments should have been processed");
            System.out.println("  [ASYNC]  Processed " + svc.getTotalEnrollments()
                + " enrollments via queue.");
            svc.shutdown();
            pass("test_async_queue_processes_requests");
        } catch (Exception e) { fail("test_async_queue_processes_requests", e); }
    }

    // ── Helpers ───────────────────────────────────────────────────────

    static void assert_equals(Object expected, Object actual, String msg) {
        if (!expected.equals(actual))
            throw new AssertionError(msg + " | expected=" + expected + " actual=" + actual);
    }

    static void assert_true(boolean condition, String msg) {
        if (!condition) throw new AssertionError(msg);
    }

    static void pass(String name) { System.out.println("  PASS  " + name); passed++; }
    static void fail(String name, Object detail) {
        System.out.println("  FAIL  " + name + " — " + detail); failed++;
    }
}
