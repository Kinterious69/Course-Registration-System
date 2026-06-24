package com.university.registration.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * ADT: Enrollment
 *
 * Represents the confirmed link between a Student and a Course.
 * Immutable after creation — an enrollment is a historical record.
 *
 * Demonstrates: immutability, value-object pattern, enum-based state.
 */
public final class Enrollment {

    public enum Status { ACTIVE, DROPPED, COMPLETED }

    // Identity 
    private final String enrollmentId;     // auto-generated UUID
    private final String studentId;
    private final String courseId;
    private final LocalDateTime enrolledAt;

    //  Status (the only mutable field — transitions are one-way)
    private Status status;

    // Constructor 

    public Enrollment(String studentId, String courseId) {
        this.enrollmentId = UUID.randomUUID().toString();
        this.studentId    = Objects.requireNonNull(studentId, "studentId must not be null");
        this.courseId     = Objects.requireNonNull(courseId, "courseId must not be null");
        this.enrolledAt   = LocalDateTime.now();
        this.status       = Status.ACTIVE;
    }

    // State transitions (validated — no invalid moves allowed) 

    /**
     * Marks this enrollment as dropped.
     * Only valid from ACTIVE state.
     */
    public void drop() {
        if (status != Status.ACTIVE)
            throw new IllegalStateException("Can only drop an ACTIVE enrollment. Current: " + status);
        this.status = Status.DROPPED;
    }

    /**
     * Marks this enrollment as completed.
     * Only valid from ACTIVE state.
     */
    public void complete() {
        if (status != Status.ACTIVE)
            throw new IllegalStateException("Can only complete an ACTIVE enrollment. Current: " + status);
        this.status = Status.COMPLETED;
    }

    //  Getters
    public String getEnrollmentId()    { return enrollmentId; }
    public String getStudentId()       { return studentId; }
    public String getCourseId()        { return courseId; }
    public LocalDateTime getEnrolledAt() { return enrolledAt; }
    public Status getStatus()          { return status; }
    public boolean isActive()          { return status == Status.ACTIVE; }

    // Object contract 

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Enrollment)) return false;
        return enrollmentId.equals(((Enrollment) o).enrollmentId);
    }

    @Override
    public int hashCode() { return enrollmentId.hashCode(); }

    @Override
    public String toString() {
        return String.format("Enrollment{id='%s', student='%s', course='%s', status=%s, at=%s}",
            enrollmentId, studentId, courseId, status, enrolledAt);
    }
}
