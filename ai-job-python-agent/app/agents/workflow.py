from collections.abc import Callable
import time
from typing import Any, TypedDict

from app.agents.nodes import (
    interview_agent,
    job_analyzer_agent,
    match_scoring_agent,
    resume_parser_agent,
    suggestion_agent,
)
from app.models.job import JobPosting
from app.models.resume import ResumeProfile

try:
    from langgraph.graph import END, StateGraph

    LANGGRAPH_AVAILABLE = True
except Exception:  # pragma: no cover
    END = None
    StateGraph = None
    LANGGRAPH_AVAILABLE = False


class WorkflowState(TypedDict, total=False):
    resume: ResumeProfile
    job: JobPosting
    resume_insight: dict[str, Any]
    job_insight: dict[str, Any]
    match_detail: dict[str, Any]
    suggestions: dict[str, Any]
    interview_questions: dict[str, Any]
    trace: list[dict[str, Any]]


class AgentWorkflow:
    def run(self, resume: ResumeProfile, job: JobPosting) -> dict[str, Any]:
        if LANGGRAPH_AVAILABLE:
            return self._run_with_langgraph(resume, job)
        return self._run_sequentially(resume, job)

    def _run_sequentially(self, resume: ResumeProfile, job: JobPosting) -> dict[str, Any]:
        trace: list[dict[str, Any]] = []
        resume_insight = self._timed(trace, "ResumeParserAgent", lambda: resume_parser_agent(resume))
        job_insight = self._timed(trace, "JobAnalyzerAgent", lambda: job_analyzer_agent(job))
        match_detail = self._timed(trace, "MatchScoringAgent", lambda: match_scoring_agent(resume_insight, job_insight))
        suggestions = self._timed(trace, "SuggestionAgent", lambda: suggestion_agent(resume_insight, job_insight, match_detail))
        interview = self._timed(trace, "InterviewAgent", lambda: interview_agent(resume_insight, job_insight, match_detail))
        return self._build_report(resume_insight, job_insight, match_detail, suggestions, interview, trace)

    def _run_with_langgraph(self, resume: ResumeProfile, job: JobPosting) -> dict[str, Any]:
        assert StateGraph is not None and END is not None
        graph = StateGraph(WorkflowState)
        graph.add_node("resume_parser", self._resume_node)
        graph.add_node("job_analyzer", self._job_node)
        graph.add_node("match_scoring", self._match_node)
        graph.add_node("suggestion", self._suggestion_node)
        graph.add_node("interview", self._interview_node)
        graph.set_entry_point("resume_parser")
        graph.add_edge("resume_parser", "job_analyzer")
        graph.add_edge("job_analyzer", "match_scoring")
        graph.add_edge("match_scoring", "suggestion")
        graph.add_edge("suggestion", "interview")
        graph.add_edge("interview", END)
        state = graph.compile().invoke({"resume": resume, "job": job, "trace": []})
        return self._build_report(
            state["resume_insight"],
            state["job_insight"],
            state["match_detail"],
            state["suggestions"],
            state["interview_questions"],
            state["trace"],
        )

    def _resume_node(self, state: WorkflowState) -> WorkflowState:
        return self._timed_state(state, "ResumeParserAgent", lambda: {"resume_insight": resume_parser_agent(state["resume"])})

    def _job_node(self, state: WorkflowState) -> WorkflowState:
        return self._timed_state(state, "JobAnalyzerAgent", lambda: {"job_insight": job_analyzer_agent(state["job"])})

    def _match_node(self, state: WorkflowState) -> WorkflowState:
        return self._timed_state(
            state,
            "MatchScoringAgent",
            lambda: {"match_detail": match_scoring_agent(state["resume_insight"], state["job_insight"])},
        )

    def _suggestion_node(self, state: WorkflowState) -> WorkflowState:
        return self._timed_state(
            state,
            "SuggestionAgent",
            lambda: {"suggestions": suggestion_agent(state["resume_insight"], state["job_insight"], state["match_detail"])},
        )

    def _interview_node(self, state: WorkflowState) -> WorkflowState:
        return self._timed_state(
            state,
            "InterviewAgent",
            lambda: {"interview_questions": interview_agent(state["resume_insight"], state["job_insight"], state["match_detail"])},
        )

    def _timed(self, trace: list[dict[str, Any]], name: str, supplier: Callable[[], dict[str, Any]]) -> dict[str, Any]:
        started = time.perf_counter()
        try:
            result = supplier()
            trace.append(
                {
                    "agent": name,
                    "status": "SUCCESS",
                    "duration_ms": int((time.perf_counter() - started) * 1000),
                    "message": "step completed",
                }
            )
            return result
        except Exception as exc:
            trace.append(
                {
                    "agent": name,
                    "status": "FAILED",
                    "duration_ms": int((time.perf_counter() - started) * 1000),
                    "message": str(exc),
                }
            )
            raise

    def _timed_state(self, state: WorkflowState, name: str, supplier: Callable[[], WorkflowState]) -> WorkflowState:
        trace = list(state.get("trace", []))
        result = self._timed(trace, name, supplier)
        return {**result, "trace": trace}

    def _build_report(
        self,
        resume: dict[str, Any],
        job: dict[str, Any],
        match: dict[str, Any],
        suggestions: dict[str, Any],
        interview: dict[str, Any],
        trace: list[dict[str, Any]],
    ) -> dict[str, Any]:
        summary = (
            f"匹配度 {match['score']} 分。"
            f"已匹配技能：{'、'.join(match.get('matched_skills') or ['暂无'])}。"
            f"主要建议：{suggestions['resume_improvements'][0]}"
        )
        return {
            "summary": summary,
            "resume_insight": resume,
            "job_insight": job,
            "match_detail": match,
            "suggestions": suggestions,
            "interview_questions": interview,
            "agent_trace": trace,
        }


agent_workflow = AgentWorkflow()
