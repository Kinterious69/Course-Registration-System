# test_recommender.py
# pytest unit tests for the recommender and analytics modules.
#
# Run with: pytest test_recommender.py -v
#
# Tests: functional pipeline correctness, edge cases, analytics accuracy,
#        higher-order function behaviour, recursive tag extraction.

import sys, os
sys.path.insert(0, os.path.dirname(__file__))

import pytest
from recommender import (
    get_eligible_courses,
    extract_tags,
    score_and_rank,
    difficulty_score,
    tag_overlap_score,
    combined_scorer,
    recommend,
)
from analytics import enrollment_summary, department_breakdown


# get_eligible_courses


class TestGetEligibleCourses:

    def test_returns_only_courses_with_met_prerequisites(self):
        """Student who completed CPS101 should see CPS201 (needs CPS101) but not CPS301."""
        eligible = get_eligible_courses(
            completed_ids=["CPS101"],
            exclude_ids=[]
        )
        ids = [c["course_id"] for c in eligible]
        assert "CPS201" in ids, "CPS201 should be eligible (prereq CPS101 met)"
        assert "CPS301" not in ids, "CPS301 needs CPS201 which is not done"
        assert "CPS314" not in ids, "CPS314 needs CPS101 + CPS201"

    def test_excludes_already_completed_courses(self):
        """Completed courses must not appear in recommendations."""
        eligible = get_eligible_courses(
            completed_ids=["CPS101"],
            exclude_ids=[]
        )
        ids = [c["course_id"] for c in eligible]
        assert "CPS101" not in ids, "CPS101 is already completed — should not be recommended"

    def test_excludes_currently_enrolled_course(self):
        """The course just enrolled in must not appear in recommendations."""
        eligible = get_eligible_courses(
            completed_ids=["CPS101"],
            exclude_ids=["CPS201"]
        )
        ids = [c["course_id"] for c in eligible]
        assert "CPS201" not in ids, "CPS201 is in exclude_ids — should not appear"

    def test_no_prereqs_course_always_eligible(self):
        """CPS101 (no prereqs) should be eligible for a student who hasn't done it."""
        eligible = get_eligible_courses(completed_ids=[], exclude_ids=[])
        ids = [c["course_id"] for c in eligible]
        assert "CPS101" in ids

    def test_returns_empty_for_completed_everything(self):
        """If all courses are completed, nothing should be returned."""
        all_ids = ["CPS101","CPS201","CPS301","CPS314","CPS401",
                   "CPS410","MTH101","MTH201","CPS320","CPS350"]
        eligible = get_eligible_courses(completed_ids=all_ids, exclude_ids=[])
        assert eligible == [], "No courses should be eligible if all are completed"

    def test_case_insensitive_completed_ids(self):
        """Lowercase completed IDs should still exclude the course."""
        eligible = get_eligible_courses(
            completed_ids=["cps101"],   # lowercase
            exclude_ids=[]
        )
        ids = [c["course_id"] for c in eligible]
        assert "CPS101" not in ids, "Completed check should be case-insensitive"


# extract_tags


class TestExtractTags:

    def test_returns_tags_for_known_course(self):
        tags = extract_tags(["CPS101"])
        assert "programming" in tags
        assert "fundamentals" in tags

    def test_returns_empty_for_unknown_course(self):
        tags = extract_tags(["UNKNOWN999"])
        assert tags == []

    def test_deduplicates_tags(self):
        """Multiple courses sharing a tag should only return the tag once."""
        tags = extract_tags(["CPS101", "CPS201"])   # both have 'programming'
        assert tags.count("programming") == 1, "Tags should be deduplicated"

    def test_empty_input_returns_empty(self):
        tags = extract_tags([])
        assert tags == []

    def test_multiple_courses_accumulate_tags(self):
        tags = extract_tags(["CPS101", "MTH101"])
        assert "programming" in tags
        assert "mathematics" in tags



# Scoring functions (higher-order functions)


class TestScoringFunctions:

    def test_difficulty_score_returns_float(self):
        course = {"difficulty": 3, "tags": []}
        score = difficulty_score(course)
        assert isinstance(score, float)
        assert 0.0 < score <= 1.0

    def test_difficulty_score_prefers_level_3(self):
        """Difficulty 3 should score higher than difficulty 1 or 5."""
        c3 = {"difficulty": 3, "tags": []}
        c1 = {"difficulty": 1, "tags": []}
        c5 = {"difficulty": 5, "tags": []}
        assert difficulty_score(c3) > difficulty_score(c1)
        assert difficulty_score(c3) > difficulty_score(c5)

    def test_tag_overlap_score_is_higher_order(self):
        """tag_overlap_score returns a function (higher-order function)."""
        scorer = tag_overlap_score(["programming"])
        assert callable(scorer), "tag_overlap_score should return a callable"

    def test_tag_overlap_score_correct_overlap(self):
        scorer = tag_overlap_score(["programming", "fundamentals"])
        course_match    = {"tags": ["programming", "fundamentals", "extra"]}
        course_no_match = {"tags": ["mathematics"]}
        assert scorer(course_match) > scorer(course_no_match)

    def test_tag_overlap_score_zero_for_no_overlap(self):
        scorer = tag_overlap_score(["programming"])
        course = {"tags": ["mathematics", "calculus"]}
        assert scorer(course) == 0.0

    def test_combined_scorer_averages_scores(self):
        """combined_scorer should average the outputs of all scorers."""
        always_one  = lambda c: 1.0
        always_zero = lambda c: 0.0
        scorer = combined_scorer(always_one, always_zero)
        result = scorer({"difficulty": 3, "tags": []})
        assert abs(result - 0.5) < 0.001, "Combined score should be 0.5 (average of 1.0 and 0.0)"

    def test_combined_scorer_single_scorer(self):
        """combined_scorer with one scorer should equal that scorer."""
        scorer = combined_scorer(difficulty_score)
        course = {"difficulty": 3, "tags": []}
        assert scorer(course) == difficulty_score(course)



# score_and_rank


class TestScoreAndRank:

    def test_returns_at_most_top_n(self):
        courses = [{"difficulty": i, "tags": []} for i in range(1, 8)]
        scorer = lambda c: c["difficulty"] / 10.0
        ranked = score_and_rank(courses, scorer, top_n=3)
        assert len(ranked) == 3

    def test_sorted_by_score_descending(self):
        courses = [{"difficulty": 1, "tags": []}, {"difficulty": 3, "tags": []}, {"difficulty": 5, "tags": []}]
        scorer = lambda c: c["difficulty"] / 10.0
        ranked = score_and_rank(courses, scorer, top_n=3)
        scores = [pair[1] for pair in ranked]
        assert scores == sorted(scores, reverse=True), "Results should be sorted descending"

    def test_returns_tuples_of_course_and_score(self):
        courses = [{"difficulty": 2, "tags": ["programming"]}]
        scorer = difficulty_score
        ranked = score_and_rank(courses, scorer, top_n=5)
        assert len(ranked) == 1
        course, score = ranked[0]
        assert isinstance(score, float)
        assert "difficulty" in course



# recommend (end-to-end pipeline)


class TestRecommend:

    def test_returns_dict_with_expected_keys(self):
        result = recommend("S001", "CPS314", ["CPS101", "CPS201"])
        assert "student_id"      in result
        assert "enrolled_course" in result
        assert "recommendations" in result
        assert "message"         in result

    def test_student_id_matches_input(self):
        result = recommend("S001", "CPS314", ["CPS101", "CPS201"])
        assert result["student_id"] == "S001"

    def test_recommendations_are_list(self):
        result = recommend("S001", "CPS314", ["CPS101", "CPS201"])
        assert isinstance(result["recommendations"], list)

    def test_enrolled_course_not_in_recommendations(self):
        """The course just enrolled in must not be recommended back."""
        result = recommend("S001", "CPS314", ["CPS101", "CPS201"])
        rec_ids = [r["course_id"] for r in result["recommendations"]]
        assert "CPS314" not in rec_ids

    def test_completed_courses_not_recommended(self):
        """Completed courses should never appear in recommendations."""
        result = recommend("S001", "CPS314", ["CPS101", "CPS201"])
        rec_ids = [r["course_id"] for r in result["recommendations"]]
        assert "CPS101" not in rec_ids
        assert "CPS201" not in rec_ids

    def test_no_eligible_courses_returns_empty(self):
        all_ids = ["CPS101","CPS201","CPS301","CPS314","CPS401",
                   "CPS410","MTH101","MTH201","CPS320","CPS350"]
        result = recommend("S001", "CPS314", all_ids)
        assert result["recommendations"] == []

    def test_at_most_5_recommendations(self):
        result = recommend("S001", "CPS101", [])
        assert len(result["recommendations"]) <= 5

    def test_recommendation_has_required_fields(self):
        result = recommend("S001", "CPS314", ["CPS101", "CPS201"])
        if result["recommendations"]:
            rec = result["recommendations"][0]
            assert "course_id"    in rec
            assert "title"        in rec
            assert "credit_hours" in rec
            assert "score"        in rec


# analytics


class TestAnalytics:

    SAMPLE = [
        {"student_id": "S001", "course_id": "CPS314", "status": "ACTIVE"},
        {"student_id": "S002", "course_id": "CPS101", "status": "ACTIVE"},
        {"student_id": "S003", "course_id": "CPS314", "status": "ACTIVE"},
        {"student_id": "S004", "course_id": "MTH101", "status": "DROPPED"},
        {"student_id": "S005", "course_id": "CPS201", "status": "ACTIVE"},
    ]

    def test_enrollment_summary_total_active(self):
        summary = enrollment_summary(self.SAMPLE)
        assert summary["total_enrollments"] == 4, "4 ACTIVE enrollments"

    def test_enrollment_summary_total_dropped(self):
        summary = enrollment_summary(self.SAMPLE)
        assert summary["total_dropped"] == 1, "1 DROPPED enrollment"

    def test_enrollment_summary_credits(self):
        summary = enrollment_summary(self.SAMPLE)
        assert summary["total_credits"] > 0, "Total credits should be positive"

    def test_enrollment_summary_top_courses(self):
        summary = enrollment_summary(self.SAMPLE)
        top_ids = [c["course_id"] for c in summary["top_courses"]]
        assert "CPS314" in top_ids, "CPS314 has 2 enrollments — should be top"

    def test_department_breakdown_returns_list(self):
        breakdown = department_breakdown(self.SAMPLE)
        assert isinstance(breakdown, list)
        assert len(breakdown) > 0

    def test_department_breakdown_sorted_descending(self):
        breakdown = department_breakdown(self.SAMPLE)
        counts = [d["count"] for d in breakdown]
        assert counts == sorted(counts, reverse=True), "Breakdown should be sorted descending"

    def test_empty_enrollments(self):
        summary = enrollment_summary([])
        assert summary["total_enrollments"] == 0
        assert summary["total_dropped"] == 0
        assert summary["total_credits"] == 0
