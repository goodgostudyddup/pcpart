# -*- coding: utf-8 -*-
import json, sys
sys.path.insert(0, r"C:\Users\yxc\Desktop\up\pcpart\agent")
from pcpart_agent.agent import PcPartAgent, CATEGORIES
from pcpart_agent.budget import budget_targets

BUDGET = int(sys.argv[1]) if len(sys.argv) > 1 else 8000
USAGE = sys.argv[2] if len(sys.argv) > 2 else "游戏"

agent = PcPartAgent()

class Spy:
    def __init__(self, inner): self.inner = inner
    def search(self, category, request=None):
        req = dict(request or {})
        try:
            data = self.inner.search(category, req)
            print(f"[SEARCH] {category} filters={req.get('filters')} price={req.get('priceCnyMin')}..{req.get('priceCnyMax')} -> total={data.get('total') if isinstance(data,dict) else '?'} records={len(data.get('records',[])) if isinstance(data,dict) else '?'}")
            return data
        except Exception as exc:
            print(f"[SEARCH-FAIL] {category} {exc}")
            raise
    def check_compatibility(self, build):
        data = self.inner.check_compatibility(build)
        print("[COMPAT] " + json.dumps(data, ensure_ascii=False)[:600])
        return data
    def get_rules(self): return self.inner.get_rules()

agent.backend = Spy(agent.backend)
targets = budget_targets(BUDGET, USAGE)
print("TARGETS:", json.dumps({k: {kk: v[kk] for kk in ("min","max","target")} for k, v in targets.items()}, ensure_ascii=False))
res = agent.recommend({"budgetCny": BUDGET, "usage": USAGE})
print("MISSING:", res["missingCategories"])
print("PARTS:", json.dumps([{ "c": p["category"], "n": p["name"], "p": p["priceCny"]} for p in res["parts"]], ensure_ascii=False, indent=1))
print("COUNT", res["metadata"]["returnedCandidatesByCategory"])
print("NOTICES:", json.dumps(res["notices"], ensure_ascii=False, indent=1))
