package com.university.registration.exception;

/**
 * Thrown when a student attempts to enroll in a course that has no available seats.
 */
public class CourseFullException extends RegistrationException {

    private final String courseId;
    private final int capacity;

    public CourseFullException(String courseId, int capacity) {
        super(
            "Course " + courseId + " is full (capacity: " + capacity + ").",
            "COURSE_FULL"
        );
        this.courseId = courseId;
        this.capacity = capacity;
    }

    public String getCourseId() { return courseId; }
    public int getCapacity()    { return capacity; }
}
