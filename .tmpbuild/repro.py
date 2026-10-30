import sys, traceback
sys.path.insert(0, r'C:\Users\yxc\Desktop\up\pcpart\agent')
from pcpart_agent.backend import BackendClient
from pcpart_agent.agent import PcPartAgent

out = open(r'C:\Users\yxc\Desktop\up\pcpart\.tmpbuild\repro-out.txt', 'w', encoding='utf-8')
out.write('stdout=' + str(sys.stdout.encoding) + ' preferred=' + str(sys.getfilesystemencoding()) + '\n')
try:
    r = BackendClient().search('cpu', {'current': 1, 'size': 10, 'requirePriced': True,
        'priceCnyMin': 1360, 'priceCnyMax': 2070, 'filters': {},
        'sortBy': 'price_cny', 'sortOrder': 'asc'})
    out.write('DIRECT_OK records=' + str(len(r.get('records', []))) + '\n')
    for row in r.get('records', [])[:3]:
        out.write('ROW ' + str(row.get('name')) + '\n')
except Exception:
    out.write('DIRECT_FAIL\n' + traceback.format_exc() + '\n')

try:
    agent = PcPartAgent()
    result = agent.recommend({'budgetCny': 10000, 'usage': '游戏'})
    out.write('RECOMMEND parts=' + str(len(result['parts'])) + ' errors=' + repr(result['queryErrors']) + '\n')
except Exception:
    out.write('RECOMMEND_FAIL\n' + traceback.format_exc() + '\n')
out.close()
