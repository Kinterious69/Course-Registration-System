# analytics.py
# Enrollment analytics — functional pipelines using map/filter/reduce.
#
# Demonstrates:
#   - map(), filter(), reduce()
#   - Lambda expressions throughout
#   - Immutable data processing
#   - Higher-order functions

from functools import reduce
from typing import Any
from courses_data import COURSE_CATALOG


def enrollment_summary(enrollments: list[dict]) -> dict:
    """
    Computes a summary of enrollment data.
    enrollments: list of {student_id, course_id, status}

    Uses filter() to separate active from dropped,
    reduce() to count credit hours, map() to extract fields.
    """
    active = list(filter(lambda e: e.get("status") == "ACTIVE", enrollments))
    dropped = list(filter(lambda e: e.get("status") == "DROPPED", enrollments))

    # Build a quick lookup: course_id -> credit_hours
    credit_map = {c["course_id"]: c["credit_hours"] for c in COURSE_CATALOG}

    # Total credit hours across all active enrollments using reduce()
    total_credits = reduce(
        lambda acc, e: acc + credit_map.get(e["course_id"], 0),
        active,
        0
    )

    # Most enrolled courses — count occurrences using reduce()
    course_counts: dict[str, int] = reduce(
        lambda acc, e: {**acc, e["course_id"]: acc.get(e["course_id"], 0) + 1},
        active,
        {}
    )

    top_courses = sorted(
        course_counts.items(),
        key=lambda pair: pair[1],
        reverse=True
    )[:3]

    return {
        "total_enrollments": len(active),
        "total_dropped":     len(dropped),
        "total_credits":     total_credits,
        "top_courses":       [{"course_id": c, "count": n} for c, n in top_courses],
    }


def department_breakdown(enrollments: list[dict]) -> list[dict]:
    """
    Groups active enrollments by department using functional pipelines.
    Returns a list sorted by enrollment count descending.
    """
    # Build course → department map
    dept_map = {c["course_id"]: c["department"] for c in COURSE_CATALOG}

    active = list(filter(lambda e: e.get("status") == "ACTIVE", enrollments))

    # Map each enrollment to its department
    departments = list(map(lambda e: dept_map.get(e["course_id"], "UNKNOWN"), active))

    # Count per department using reduce()
    counts: dict[str, int] = reduce(
        lambda acc, dept: {**acc, dept: acc.get(dept, 0) + 1},
        departments,
        {}
    )

    return sorted(
        [{"department": d, "count": n} for d, n in counts.items()],
        key=lambda x: x["count"],
        reverse=True
    )
