from pydantic import BaseModel, Field


class TelemetryPingRequest(BaseModel):
    event_type: str = Field(default="app_open", max_length=32)


class TelemetryPingResponse(BaseModel):
    accepted: bool
    deduped: bool
    event_type: str
