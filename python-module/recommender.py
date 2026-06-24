# recommender.py
# Course recommendation engine — purely functional style.
#
# Demonstrates:
#   - Lambda expressions
#   - Higher-order functions (functions that accept/return functions)
#   - map(), filter(), sorted() with lambdas
#   - Functional composition
#   - Recursion
#   - Immutable data (tuples, no mutation)
#   - functools.reduce()

from functools import reduce
from typing import Callable
from courses_data import COURSE_CATALOG


# Scoring strategies (higher-order functions) 
#
# Each strategy is a function that takes a course dict and returns a float.
# You can pass any strategy into recommend() — this is the functional
# interface / strategy pattern in Python.

def difficulty_score(course: dict) -> float:
    """Prefer courses close to difficulty level 3 (challenging but doable)."""
    return 1.0 / (1.0 + abs(course["difficulty"] - 3))


def tag_overlap_score(target_tags: list[str]) -> Callable[[dict], float]:
    """
    Returns a scoring function that scores courses by tag overlap
    with the given tags. This is a higher-order function — it takes
    a list and returns a function (closure).
    """
    return lambda course: len(set(course["tags"]) & set(target_tags)) / max(len(target_tags), 1)


def combined_scorer(*scorers: Callable[[dict], float]) -> Callable[[dict], float]:
    """
    Functional composition: combines multiple scoring functions into one
    by averaging their scores. Takes any number of scorer functions.

    Uses reduce() to sum scores, then divides by count.
    """
    return lambda course: reduce(
        lambda total, scorer: total + scorer(course),
        scorers,
        0.0
    ) / len(scorers)


# Core pipeline 

def get_eligible_courses(completed_ids: list[str], exclude_ids: list[str]) -> list[dict]:
    """
    Returns courses the student is eligible for but hasn't taken.

    Uses filter() with a lambda — purely functional, no for-loops.
    completed_ids and exclude_ids are treated as immutable (never modified).
    """
    completed_upper = list(map(lambda c: c.upper(), completed_ids))
    exclude_upper   = list(map(lambda c: c.upper(), exclude_ids))

    return list(filter(
        lambda course: (
            # Not already completed
            course["course_id"] not in completed_upper
            # Not currently enrolled in
            and course["course_id"] not in exclude_upper
            # All prerequisites satisfied
            and all(prereq in completed_upper for prereq in course["prerequisites"])
        ),
        COURSE_CATALOG
    ))


def extract_tags(course_ids: list[str]) -> list[str]:
    """
    Recursively extracts all tags from a list of course IDs.
    Uses recursion instead of a loop — functional style.
    """
    catalog_map = {c["course_id"]: c for c in COURSE_CATALOG}

    def recurse(ids: list[str], accumulated: list[str]) -> list[str]:
        if not ids:                          # base case
            return accumulated
        course = catalog_map.get(ids[0], {})
        new_tags = course.get("tags", [])
        return recurse(ids[1:], accumulated + new_tags)   # tail recursion

    return list(set(recurse(course_ids, [])))   # deduplicate with set


def score_and_rank(
    courses: list[dict],
    scorer: Callable[[dict], float],
    top_n: int = 5
) -> list[tuple[dict, float]]:
    """
    Scores each course using the given scorer function,
    sorts by score descending, returns top N as (course, score) tuples.

    Uses map() to score, sorted() with lambda key, slicing for top N.
    All immutable — original list is never modified.
    """
    scored = list(map(
        lambda course: (course, round(scorer(course), 4)),
        courses
    ))
    return sorted(scored, key=lambda pair: pair[1], reverse=True)[:top_n]


# Public API

def recommend(
    student_id: str,
    enrolled_course: str,
    completed_courses: list[str]
) -> dict:
    """
    Main recommendation function called by FastAPI.

    Pipeline:
      1. Extract tags from completed courses
      2. Filter to eligible courses
      3. Build a combined scorer from difficulty + tag overlap
      4. Score and rank
      5. Return top 5 as structured response

    Entirely functional — no classes, no mutation, no loops.
    """
    # Step 1 — extract interest tags from what the student has done
    interest_tags = extract_tags(completed_courses)

    # Step 2 — filter to eligible courses (not done, prereqs met)
    eligible = get_eligible_courses(
        completed_ids=completed_courses,
        exclude_ids=[enrolled_course]
    )

    if not eligible:
        return {
            "student_id": student_id,
            "enrolled_course": enrolled_course,
            "recommendations": [],
            "message": "No eligible courses found."
        }

    # Step 3 — compose a scorer: 50% difficulty fit + 50% tag overlap
    scorer = combined_scorer(
        difficulty_score,
        tag_overlap_score(interest_tags)
    )

    # Step 4 — score and rank
    ranked = score_and_rank(eligible, scorer, top_n=5)

    # Step 5 — format response using map()
    recommendations = list(map(
        lambda pair: {
            "course_id":   pair[0]["course_id"],
            "title":       pair[0]["title"],
            "credit_hours": pair[0]["credit_hours"],
            "difficulty":  pair[0]["difficulty"],
            "score":       pair[1],
        },
        ranked
    ))

    return {
        "student_id":      student_id,
        "enrolled_course": enrolled_course,
        "recommendations": recommendations,
        "message":         f"{len(recommendations)} courses recommended."
    }
