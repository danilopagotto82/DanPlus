from pathlib import Path
import csv,re,subprocess,collections
root=Path('docs/danplus')
audit=root/'auditoria'
def table(path):
    with path.open(encoding='utf-8-sig',newline='') as f:
        return {row['ID']:row for row in csv.DictReader(f)}
historical=table(audit/'DANPLUS_2_1_0_MATRIZ_146_DECISOES.csv')
e00=table(audit/'E00_GITHUB_AUDITORIA_EVIDENCIAS_166_20261122.csv')
ml07=table(audit/'HIST_01_BACKLOG_ML07.csv')
main=table(root/'MATRIZ_EXECUCAO_2_1_2.csv')
complement=table(root/'MATRIZ_COMPLEMENTAR_ML05_S.csv')
assert len(historical)==146 and len(e00)==166 and len(ml07)==194
assert len(main)==34 and len(complement)==10
assert set(historical)<=set(e00)<=set(ml07)
def git(*args):
    return subprocess.check_output(['git',*args],encoding='utf-8').strip()
tracked=set(git('ls-files').splitlines())
checkpoint_paths=sorted(root.glob('CHECKPOINT*.md'))
source_paths=checkpoint_paths+[root/'INDICE_MUD.md']+list((root/'gemini-planejamento').glob('DP_CODEX_04*'))
indexed={p: p.read_text(encoding='utf-8-sig').splitlines() for p in source_paths}
patches={}
history=git('log','--format=%h','--name-only','5cb4f2d..HEAD')
rev=''
for line in history.splitlines():
    if re.fullmatch(r'[0-9a-f]{7,12}',line):
        rev=line
    elif line and rev:
        patches.setdefault(line, []).append(rev)
check={
    'B1-27':('IMPLEMENTADO_COM_EVIDENCIA','SourceFailoverSelection.selectNext; SourceFailoverSelectionTest; ServerSourceFailoverTest','ML08: physical mixed source chain'),
    'B1-16':('IMPLEMENTADO_NAO_HOMOLOGADO','PlayerRuntimeControllerReusedLink.refreshLink; ReusedLinkRefreshTest; 401/410 at runtime','ML08: real HTTP/Debrid + tracks and resumed position'),
    'B1-03':('IMPLEMENTADO_NAO_HOMOLOGADO','HomeViewModelWatchedTitles.observeWatchedTitlesPipeline; HomeWatchedTitlesPipelineTest','ML08: every requested surface, offline and reconnection'),
    'B1-11':('NAO_COMPROVADO','TrackingNextUpSeedPolicyTest; CHECKPOINT_E06: active route after process death requires reproduction','ML08: Android process restart and exact episode'),
    'B1-19':('IMPLEMENTADO_NAO_HOMOLOGADO','SearchScreen and SearchViewModel retain row/item identity','ML08: DPAD, filter and reload'),
    'B1-21':('IMPLEMENTADO_NAO_HOMOLOGADO','LibraryScreen: actual boolean from Compose focus request; default focus when unavailable','ML08: DPAD, deleted item and return after playback'),
    'A2-08':('ESTUDO','AudioPassthroughPolicy and PlayerAudioFallbackPolicy; hardware-specific transport','ML08: Shield receiver, Fire TV soundbar'),
    'A2-11':('ESTUDO','PlayerAudioFallbackPolicyTest; MPV unchanged','ML08: pause, resume, recovery and track switching'),
    'A2-32':('NAO_COMPROVADO','HomePosterLoadingTest protects current prefetch; latency not reproduced','ML08: cold/warm measurement by surface'),
    'RSQ-09':('IMPLEMENTADO_COM_EVIDENCIA','docs/danplus/tools/github; fixtures plus live read-only inventory','Remote activation / protection depends on explicit gate'),
    'MUD-002':('IMPLEMENTADO_NAO_HOMOLOGADO','AppReportTest; ReportTextTest; ReportOutboxTest; CHECKPOINT_E05 and E08','Test two accounts against non-production backend'),
    'MUD-012':('IMPLEMENTADO_NAO_HOMOLOGADO','SpecialHighlightSync; CollectionsDataStore; existing revision RPC; CHECKPOINT_E08','Two authorized clients and collection removal'),
    'MUD-011':('BLOQUEADO','RS-29: manual backup and mocked Drive protocol','Google Cloud OAuth and concurrent write proof'),
    'REQ16-09':('EXCLUIDO','User decision: no duplicate Supabase backup','Keep the historical record'),
    'REQ16-14':('IMPLEMENTADO_NAO_HOMOLOGADO','CHECKPOINT_E09 documents trailer selector and fallback tests','ML08: Minority Report and A.I. with DPAD'),
    'A1-10':('DUPLICADO','B1-16: refresh of expired source','Follow the B1-16 gate'),
    'A2-13':('DUPLICADO','B1-16 and B1-27: expired Debrid and eligible fallback','Follow the canonical playback gates'),
    'B2-07':('DUPLICADO','B1-27: bounded failover for failed sources','Follow the B1-27 gate')
}
supported_by_checkpoint = {
    'CHECKPOINT_E03.md': 'B1-07 B1-13 A2-09'.split(),
    'CHECKPOINT_E04.md': 'A1-23 A2-02 A2-21 A2-25 A2-26 A2-27 A2-29 RSQ-05'.split(),
    'CHECKPOINT_E05.md': 'MUD-004 MUD-005 MUD-006 MUD-008 MUD-013 REQ16-02 REQ16-05 REQ16-06 A1-26 A1-40'.split(),
    'CHECKPOINT_E06.md': 'B1-18 B1-24 A1-09 A1-22 A2-16 A2-38 A2-44 A2-48 B4-D05'.split(),
    'CHECKPOINT_E08.md': 'B2-11 A1-05 A1-37 A1-41 A2-50 RSQ-08'.split(),
    'CHECKPOINT_E09.md': 'A2-45 B3-D08 B4-D17'.split(),
}
for filename, ids in supported_by_checkpoint.items():
    document = root / filename
    for key in ids:
        needle=re.compile(r'(?<![0-9A-Z-])'+re.escape(key)+r'(?![0-9A-Z-])')
        matches=[(n+1,line) for n,line in enumerate(indexed[document])
                 if line.startswith("| ") and needle.search(line.split("|")[1])]
        if matches:
            line_no,line=matches[0]
            snippet=line.split("|")[2].strip()
            check.setdefault(key,(
                "IMPLEMENTADO_NAO_HOMOLOGADO", f"{filename}:{line_no} \u2014 {snippet}",
                "Revalidar o escopo exato no gate f\u00edsico ou de integra\u00e7\u00e3o existente."
            ))
check.update({
    "MUD-001":("IMPLEMENTADO_NAO_HOMOLOGADO","CHECKPOINT_E07A.md:12; identidade do app, manifest e recursos.","Conferir launcher e atualizar mantendo package/certificado."),
    "REQ16-01":("DUPLICADO","MUD-001: identidade e nome DanPlus.","Ver gate MUD-001."),
    "REQ16-04":("DUPLICADO","MUD-013: botões e status real do Seekr.","Ver gate MUD-013."),
    "REQ16-07":("DUPLICADO","MUD-013: sinalização do uso do Seekr no player.","Ver gate MUD-013."),
    "REQ16-08":("IMPLEMENTADO_NAO_HOMOLOGADO","CHECKPOINT_E07A.md:14; localização do HUD.","Conferir estatísticas PT-BR e dados técnicos em ambas TVs."),
    "MUD-012":("IMPLEMENTADO_COM_EVIDENCIA",
               "SpecialHighlightSync; CollectionsDataStore; RPC exercises selection, conflict, removal, role scope and rollback.",
               "Confirmar propaga\u00e7\u00e3o em dois clientes autorizados e remo\u00e7\u00e3o da cole\u00e7\u00e3o."),
})
classification={
    'EXCLUIDO':'EXCLUIDO','FORA_DE_ESCOPO':'EXCLUIDO','LAB_OPCIONAL':'ESTUDO',
    'GAP_DE_CONTRATO_COMPROVADO':'GAP_COMPROVADO','AUSENCIA_VERIFICADA_NA_ARVORE':'GAP_COMPROVADO',
    'PRESERVAR_QA':'ADIADO',
}
fields=['ID','ORIGEM','DECISAO_ORIGINAL','CLASSIFICACAO_ML07_DEFINITIVO','EVIDENCIA_HISTORICA','VINCULOS_ORIGINAIS',
        'CAMINHOS_VIGENTES','CHECKPOINTS','IDS_MATRIZES','COMMITS_COM_MUDANCA_NOS_CAMINHOS',
        'EVIDENCIA_ATUAL','GATE_RESTANTE']
out=audit/'HIST_01_RECONCILIACAO_DEFINITIVA.csv'
states=collections.Counter()
with out.open('w',encoding='utf-8',newline='') as f:
    writer=csv.DictWriter(f,fields,lineterminator='\n')
    writer.writeheader()
    for key,orig in ml07.items():
        old=e00.get(key,{})
        canonical=historical.get(key,{})
        evidence=old.get('Evidencia_ou_Limite') or orig['EVIDENCIA']
        evidence=evidence.replace('hist?rico','hist\u00f3rico').replace(' ? ',' \u2014 ').replace('N?O','N\u00c3O')
        declared=old.get('Arquivo_Fonte') or orig['ARQUIVO_DECLARADO']
        paths=sorted({p.strip() for p in re.split(r'[;|,]',declared) if p.strip() in tracked})
        needle=re.compile(r'(?<![0-9A-Z-])'+re.escape(key)+r'(?![0-9A-Z-])')
        found={p.name:[str(i+1) for i,line in enumerate(lines) if needle.search(line)] for p,lines in indexed.items()}
        related=sorted(rowid for rowid,row in (main|complement).items() if rowid==key or any(needle.search(value or '') for value in row.values()))
        state,proof,gate=check.get(key,(
            classification.get(old.get('Estado_E00_G',''),'NAO_COMPROVADO'),
            'Evidência histórica vinculada. A prova atual do requisito específico permanece em aberto.',
            (old.get('Proxima_Acao') or orig['PROXIMO_GATE'])
        ))
        states[state]+=1
        writer.writerow(dict(zip(fields,[
            key,orig['ORIGEM'],old.get('Decisao') or orig['DECISAO'].replace('hist?rico','hist\u00f3rico'),state,evidence,
            canonical.get('Relacionamento',''),'; '.join(paths),
            '; '.join(name+':'+','.join(lines) for name,lines in found.items() if lines),
            '; '.join(related),'; '.join(dict.fromkeys(rev for path in paths for rev in patches.get(path,[]))),
            proof,gate
        ])))
print(dict(sorted(states.items())))
