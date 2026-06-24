package com.university.registration.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * ADT: Course
 *
 * Represents a university course with strong encapsulation.
 * All mutation goes through validated methods — no raw field access.
 *
 * Demonstrates: encapsulation, immutable collections, defensive copying.
 */
public class Course implements StudentRegistry.Registrable {

    public enum Department {
        COMPUTER_SCIENCE, MATHEMATICS, PHYSICS, ENGINEERING, BUSINESS
    }

    // Identity
    private final String courseId;       // e.g. "CPS314"
    private final String title;
    private final Department department;
    private final int creditHours;

    // ── Capacity (mutable, protected by synchronized methods) ─────────
    private final int maxCapacity;
    private int enrolledCount;

    //  Academic rules
    private final List<String> prerequisiteCourseIds;   // course IDs that must be completed first

    //  Constructors 
    public Course(String courseId, String title, Department department,
                  int creditHours, int maxCapacity, List<String> prerequisites) {
        if (courseId == null || courseId.isBlank())
            throw new IllegalArgumentException("courseId must not be blank.");
        if (maxCapacity <= 0)
            throw new IllegalArgumentException("maxCapacity must be positive.");
        if (creditHours < 1 || creditHours > 6)
            throw new IllegalArgumentException("creditHours must be between 1 and 6.");

        this.courseId   = courseId.toUpperCase();
        this.title      = Objects.requireNonNull(title, "title must not be null");
        this.department = Objects.requireNonNull(department, "department must not be null");
        this.creditHours = creditHours;
        this.maxCapacity = maxCapacity;
        this.enrolledCount = 0;
        this.prerequisiteCourseIds = prerequisites == null
            ? new ArrayList<>()
            : new ArrayList<>(prerequisites);   // defensive copy
    }

    /** Convenience constructor — no prerequisites */
    public Course(String courseId, String title, Department department,
                  int creditHours, int maxCapacity) {
        this(courseId, title, department, creditHours, maxCapacity, List.of());
    }

    // Seat management (synchronized for thread safety) 

    /**
     * Attempts to reserve one seat.
     * @return true if a seat was successfully reserved, false if course is full.
     */
    public synchronized boolean reserveSeat() {
        if (enrolledCount >= maxCapacity) return false;
        enrolledCount++;
        return true;
    }

    /**
     * Releases one seat (e.g. when a student drops the course).
     */
    public synchronized void releaseSeat() {
        if (enrolledCount > 0) enrolledCount--;
    }

    //  Queries

    public synchronized boolean hasAvailableSeats() {
        return enrolledCount < maxCapacity;
    }

    public synchronized int getAvailableSeats() {
        return maxCapacity - enrolledCount;
    }

    public synchronized int getEnrolledCount() {
        return enrolledCount;
    }

    //  Getters (immutable fields need no sync) 

    @Override
    public String getId()                          { return courseId; }
    public String getCourseId()                    { return courseId; }
    public String getTitle()                       { return title; }
    public Department getDepartment()              { return department; }
    public int getCreditHours()                    { return creditHours; }
    public int getMaxCapacity()                    { return maxCapacity; }

    /** Returns an unmodifiable view — callers cannot mutate the list. */
    public List<String> getPrerequisiteCourseIds() {
        return Collections.unmodifiableList(prerequisiteCourseIds);
    }

    public boolean hasPrerequisites() {
        return !prerequisiteCourseIds.isEmpty();
    }

    // Object contract 

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Course)) return false;
        return courseId.equals(((Course) o).courseId);
    }

    @Override
    public int hashCode() { return courseId.hashCode(); }

    @Override
    public String toString() {
        return String.format("Course{id='%s', title='%s', dept=%s, seats=%d/%d}",
            courseId, title, department, enrolledCount, maxCapacity);
    }
}
