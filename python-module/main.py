# main.py
# FastAPI application — exposes endpoints that the Java backend calls.
#
# Run with:  uvicorn main:app --reload --port 8000
#
# Endpoints:
#   POST /recommend    → called by Java after each enrollment
#   POST /analytics    → called by Java to get enrollment stats
#   GET  /health       → Java checks this to see if Python is up

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
import recommender
import analytics

app = FastAPI(title="Course Registration — Python Analytics Module")


# Request / Response models 

class RecommendRequest(BaseModel):
    student_id: str
    enrolled_course: str
    completed_courses: list[str] = []


class AnalyticsRequest(BaseModel):
    enrollments: list[dict]


# Endpoints 

@app.get("/health")
def health():
    """Java calls this to verify Python module is running."""
    return {"status": "ok", "module": "course-analytics"}


@app.post("/recommend")
def get_recommendations(req: RecommendRequest):
    """
    Called by Java's RegistrationService after a successful enrollment.
    Returns up to 5 recommended courses for the student.
    """
    try:
        result = recommender.recommend(
            student_id=req.student_id,
            enrolled_course=req.enrolled_course,
            completed_courses=req.completed_courses
        )
        return result
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/analytics")
def get_analytics(req: AnalyticsRequest):
    """
    Accepts a list of enrollment records and returns summary statistics.
    """
    try:
        summary   = analytics.enrollment_summary(req.enrollments)
        breakdown = analytics.department_breakdown(req.enrollments)
        return {"summary": summary, "by_department": breakdown}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
