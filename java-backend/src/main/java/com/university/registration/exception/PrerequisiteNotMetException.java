package com.university.registration.exception;

import java.util.List;

/**
 * Thrown when a student has not completed the required prerequisites for a course.
 */
public class PrerequisiteNotMetException extends RegistrationException {

    private final String courseId;
    private final List<String> missingPrerequisites;

    public PrerequisiteNotMetException(String courseId, List<String> missingPrerequisites) {
        super(
            "Student has not completed prerequisites for " + courseId
                + ": " + missingPrerequisites,
            "PREREQUISITE_NOT_MET"
        );
        this.courseId = courseId;
        this.missingPrerequisites = List.copyOf(missingPrerequisites);
    }

    public String getCourseId()                    { return courseId; }
    public List<String> getMissingPrerequisites()  { return missingPrerequisites; }
}
