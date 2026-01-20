import React from "react";
import { Handle, Position } from "reactflow";

export default function PostItNode({ data }) {
    const { label, role, capabilityType, requires, provides } = data;
    const isMicro = role === "MicroClient";
    const isCap = role === "Capability";

    return (
        <div
            className={[
                "postit",
                isMicro && "microclient",
                isCap && "capability",
                isCap && capabilityType && capabilityType.toLowerCase(),
            ]
                .filter(Boolean)
                .join(" ")}
        >
            <div className="title">{label}</div>
            <div className="subtitle">
                {isMicro ? "MicroClient" : capabilityType || "Capability"}
            </div>

            {requires && (
                <div className="list-line">
                    <span>Req:</span>{" "}
                    {requires.length ? requires.join(", ") : "—"}
                </div>
            )}

            {provides && (
                <div className="list-line">
                    <span>Prov:</span>{" "}
                    {provides.length ? provides.join(", ") : "—"}
                </div>
            )}

            <Handle type="source" position={Position.Right} />
            <Handle type="target" position={Position.Left} />
        </div>
    );
}