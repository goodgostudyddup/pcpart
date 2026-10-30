# -*- coding: utf-8 -*-
import sys, json
sys.path.insert(0, r"C:\Users\yxc\Desktop\up\pcpart\agent")
from pcpart_agent.agent import PcPartAgent

for budget, usage in ((8000, "游戏"), (3000, "办公")):
    print("=" * 20, budget, usage)
    agent = PcPartAgent()
    s = agent.create_session()
    r = agent.chat(s["sessionId"], f"预算 {budget} 元，主要{usage}，帮我推荐一台整机")
    print(r.get("reply"))
    print("MISSING:", r["backendData"]["missingCategories"], "| relaxed:", r["backendData"]["queryMeta"]["relaxedCategories"], "| swaps:", r["backendData"]["metadata"]["caseSwaps"])
