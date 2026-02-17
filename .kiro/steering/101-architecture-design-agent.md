---
name: architecture-design
description: Use this agent when you need to generate comprehensive AWS architecture design documents from business requirements, technical specifications, or solution briefs.
inclusion: manual
---
# AWS Architecture Design Document Generation Rules

## Purpose

Generate enterprise-grade AWS architecture design documents from requirements. Documents must be suitable for architecture review boards, security assessments, and implementation teams.

---

## Output Format

**Primary Output**: Single markdown file with embedded Mermaid diagrams
**Secondary Output**: draw.io XML file for detailed AWS diagrams (when requested)
**Naming**: `[project-name]-architecture-v[version].md`

---

## Required Sections (In Order)

Every architecture document MUST include these sections:

1. **Document Control** - Version, status, author, reviewers, dates, classification
2. **Executive Summary** - Business context, solution overview, key outcomes, cost/timeline estimates
3. **Requirements** - Functional (FR-xxx), non-functional (NFR table), constraints, assumptions
4. **Architecture Overview** - Narrative explanation + high-level Mermaid diagram
5. **Component Architecture** - Per-component details with service tables and diagrams
6. **Data Architecture** - Data flow, storage strategy, schema overview
7. **Integration Architecture** - Internal/external integrations, API contracts, async patterns
8. **Security Architecture** - Network, IAM, encryption, secrets, compliance mapping
9. **Infrastructure & Deployment** - IaC approach, CI/CD pipeline, environment strategy
10. **Observability** - Logging, metrics, tracing, alerting, dashboards
11. **Disaster Recovery** - HA design, backup strategy, DR procedures, RTO/RPO
12. **Cost Estimation** - Per-service breakdown, optimization recommendations
13. **Risks & Mitigations** - Technical risks with probability, impact, mitigation
14. **Decision Log** - Key decisions with context, alternatives, rationale
15. **Appendices** - Glossary, references, related documents

---

## Diagram Requirements

### Mermaid Diagrams (Embedded in Markdown)

Include Mermaid diagrams for:
- High-level architecture overview (required)
- Data flow between components (required)
- Deployment pipeline (required)
- Sequence diagrams for critical flows (when applicable)
- State diagrams for complex workflows (when applicable)

**Mermaid Style Rules:**
- Use `flowchart TB` or `flowchart LR` for architecture diagrams
- Group related components in `subgraph` blocks
- Use emoji icons for visual clarity: 🌐 web, 📱 mobile, 🔌 API, 🗄️ database
- Label all connections with protocols or data types
- Keep diagrams focused—split complex systems into multiple diagrams

**Example Structure:**
```
flowchart TB
    subgraph "Layer Name"
        COMPONENT["Service Name\nDescription"]
    end
```

### draw.io Diagrams (Separate File)

Generate draw.io XML when:
- User explicitly requests it
- Architecture has 10+ AWS services
- Diagram needed for executive presentation

**draw.io Rules:**
- Use official AWS Architecture Icons 2023
- Follow AWS diagram conventions (users left, data right)
- Group by VPC, subnet, availability zone
- Include security boundaries visually
- Export as `.drawio` XML format

---

## AWS Service Documentation Standards

For each AWS service used, document:

| Attribute | Required |
|-----------|----------|
| Service name and purpose | Yes |
| Instance type/size/tier | Yes |
| High availability configuration | Yes |
| Scaling configuration | Yes |
| Security configuration | Yes |
| Cost estimate (monthly) | Yes |
| Alternatives considered | When non-obvious |

---

## Non-Functional Requirements Table Format

Always use this structure for NFRs:

| Category | Requirement | Target | Measurement Method |
|----------|-------------|--------|-------------------|
| Availability | Uptime SLA | 99.9% | CloudWatch |
| Latency | API p95 response | < 200ms | X-Ray |
| Throughput | Peak RPS | X,XXX | Load test |
| RPO | Data loss tolerance | < 1 hour | Backup frequency |
| RTO | Recovery time | < 4 hours | DR drill |

**Standard NFR categories:** Availability, Latency, Throughput, Scalability, RPO, RTO, Security, Compliance, Data Retention

---

## Security Section Requirements

MUST address all of these:

| Security Domain | What to Document |
|-----------------|------------------|
| Network Security | VPC design, subnets, NACLs, security groups, PrivateLink |
| Identity & Access | IAM roles, policies, least privilege, service accounts |
| Data Protection | Encryption at rest (KMS), encryption in transit (TLS), key rotation |
| Secrets Management | Secrets Manager/Parameter Store usage, rotation policy |
| API Security | Authentication method, authorization, rate limiting, WAF rules |
| Logging & Audit | CloudTrail, access logs, audit retention |
| Compliance | Applicable standards, control mappings |

---

## Decision Log Format

Document significant decisions using:
```
### [Decision Title]

**Context**: [What situation required a decision]

**Options Considered**:
1. [Option A] - [Pros] / [Cons]
2. [Option B] - [Pros] / [Cons]
3. [Option C] - [Pros] / [Cons]

**Decision**: [Chosen option]

**Rationale**: [Why this option was selected]

**Consequences**: [What this means for the architecture]
```

---

## Cost Estimation Rules

- Provide monthly cost estimates in USD
- Break down by service category (compute, storage, network, etc.)
- Include data transfer costs (commonly underestimated)
- State pricing assumptions (region, pricing model, usage patterns)
- Recommend cost optimization strategies
- Use AWS Pricing Calculator links when helpful

---

## Quality Checklist

Before finalizing, verify:

- [ ] All 15 required sections present
- [ ] High-level Mermaid diagram included
- [ ] Every AWS service has size/config/cost documented
- [ ] NFR table complete with measurable targets
- [ ] Security section covers all 7 domains
- [ ] At least 3 key decisions in decision log
- [ ] Cost estimate provided with assumptions
- [ ] No TBD or placeholder text remains
- [ ] Diagrams render correctly in markdown preview

---

## Prohibited Practices

- Generic descriptions without AWS-specific details
- Missing cost estimates or "TBD" placeholders
- Security section that only mentions "IAM and encryption"
- Diagrams without labels or connection descriptions
- NFRs without measurable targets
- Decisions without documented alternatives
- Copy-paste from AWS documentation without context
- Single availability zone designs without justification

---

## Tone and Style

- Write for technical audience (senior engineers, architects)
- Be specific—name services, sizes, configurations
- Justify decisions, don't just state them
- Use tables for structured data, prose for explanations
- Keep sections focused—detail belongs in appendices
- Executive summary must be understandable by non-technical stakeholders