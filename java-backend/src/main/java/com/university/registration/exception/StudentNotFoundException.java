package com.university.registration.exception;

/**
 * Thrown when a student ID does not exist in the registry.
 */
public class StudentNotFoundException extends RegistrationException {

    private final String studentId;

    public StudentNotFoundException(String studentId) {
        super("Student with ID '" + studentId + "' was not found.", "STUDENT_NOT_FOUND");
        this.studentId = studentId;
    }

    public String getStudentId() { return studentId; }
}
