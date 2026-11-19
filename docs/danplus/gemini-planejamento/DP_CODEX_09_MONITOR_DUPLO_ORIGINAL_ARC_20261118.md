# DANPLUS · DP-CODEX-09 — MONITOR DUPLO NUVIO ORIGINAL + NUVIO ARC
Data: 18/11/2026. STATUS: **ESPECIFICAÇÃO; NÃO IMPLEMENTADO.** Complementa o DP-CODEX-08. Nenhuma permissão para alterar código, GitHub, release, OTA ou aparelhos nesta etapa.

## 1. Correção de origem aprovada pelo proprietário
Fontes OBRIGATÓRIAS e independentes:
- NUVIO ORIGINAL (upstream oficial): `NuvioMedia/NuvioTV` — https://github.com/NuvioMedia/NuvioTV/releases
- NUVIO ARC (fork de origem da linhagem do DanPlus): `ysosrs123/NuvioTV-Fork` — https://github.com/ysosrs123/NuvioTV-Fork/releases
- DANPLUS (verdade de execução e fonte de comparação): worktree protegida local `D:\APK\Worktrees\DanPlus_2_0_Unificado_ML01_20261115`. O ARC é base de LINHAGEM, **não substitui a baseline real do código DanPlus instalado**.
- NUVIO RESHAPED: terceira fonte OPCIONAL apenas para observações/ideias já separadas sob RS-01..RS-07; NÃO substituir ou confundir com ARC. Exige decisão futura para tornar monitor obrigatório.
Tanto releases estáveis quanto pré-lançamentos devem entrar no inventário; drafts não. Detectar TODAS as releases inéditas de cada fonte, não apenas uma versão "latest".

## 2. O que comparar — visão de três vias
(A) Nuvio Original VELHO → Nuvio Original NOVO: quais novidades e consertos oficiais surgiram.
(B) Nuvio ARC VELHO → Nuvio ARC NOVO: novidades específicas do fork e componentes herdados do original.
(C) Novo evento em qualquer fonte → código REAL DanPlus: o que já existe, foi adaptado, conflita ou merece proposta.
Para cada recurso visto primeiro no Original e depois no ARC, relacionar evidências (arquivos/commits/patch-id quando comparável) e contar **uma só oportunidade funcional**, preservando as duas origens. Sem ancestry comprovado, não assumir que versões de nomes parecidos são equivalentes. Só fazer comparação profunda do DanPlus em ambiente/local privado autorizado, nunca exportar código, segredos, .wrangler ou signing keys para runner público.

## 3. DA-00 — Observação barata por fonte (primeiro estágio)
- Consultar GitHub Releases API dos DOIS repositórios (e tags se necessário), com paginação e backfill, respeitando rate limit, ETag e repetição.
- Identidade imutável do evento: source_repo + release_id + tag_name + tag_commit_sha; registrar published_at, prerelease, URL, changelog e data da consulta. Se a mesma tag apontar para SHA alterado, ALERTA DE INTEGRIDADE/HOLD, sem processar como atualização normal.
- Persistir marcadores separados para Original e ARC e ledger de eventos, evitando perder segunda release entre polls, inclusive reexecuções concorrentes. Não considerar "sem novidades" se alguma fonte deu erro (403, 404, 429, 5xx, timeout): relatório PARCIAL/FAIL e tentativa futura.
- Agendamento futuro leve, configurável (sugestão inicial 2x ao dia), mais execução manual; verificar branch DEFAULT real, permissões, heartbeat do monitor e ausência de cron. Um run manual não comprova schedule.
- Monitor de notícias/release não executa Gradle, não abre PR e não roda agentes de IA. Permissões somente leitura. Não usar token do player, OTA, Supabase, keystore ou GitHub de publicação.

## 4. DA-01 — Relatório automático e reconciliação do catálogo
Gerar por nova release: fonte exata, nome, tag, SHA, stable/beta, data, changelog, link, diff e lista de arquivos/commits verificados (ou marcar NÃO VERIFICADO), grupos UI/DPAD/Compose, player/áudio/legendas, Fire 32-bit/memória, Seekr, Trakt, backend, OTA/Gradle e segurança.
Cruzar, SEM ALTERAR decisões humanas, os 146 IDs da auditoria (36 decisões + 110 pendentes), 7 RS (6 decididos, RS-06 pendente), 14 MUD R3, pedidos de 16/11, BUG-THUMB-01, P0 Detalhes e ponte OTA ML-C1 separada. B2-06, RS-06 e 110 pendentes permanecem HOLD. Decisões INVESTIGAR nunca autorizam patch.
Classificações possíveis: JÁ PRESENTE; CANDIDATO A ADAPTAR; INVESTIGAR; REDUNDANTE; INCOMPATÍVEL/RISCO; PENDENTE DE DECISÃO; NÃO VERIFICADO. Anexar justificativa e evidência; não inferir equivalência só de changelog; diferença da UI exige screenshot físico para ser considerada validada.
Criar relatório-mestre consolidado por execução, com duas seções (Original e ARC) e uma lista de oportunidades deduplicadas. Quando uma feature existe nas duas fontes, agregar provas sem duplicar trabalho ou mudar seu ID canônico.

## 5. DA-02, DA-03, DA-04 — SOMENTE DEPOIS DE GATES
DA-02: issues/PR DRAFT ou protótipo em branch/worktree isolada, sem autopatch, somente com autorização de escrita no repositório correto e decisões já aprovadas. Conflito => HOLD, jamais forçar merge.
DA-03: teste unitário, lint, CI focado e eventualmente build DEBUG em ambiente seguro SEM segredos de assinatura; executar só em candidato aprovado, não em cada consulta de releases.
DA-04: QA manual Shield/Fire, screenshots do app versus aprovado, playback, fontes, codecs, áudio sem perdas, perfis e OTA. Nada de assinatura, instalação, merge, lançamento, Supabase remoto ou produção sem gate humano próprio.

## 6. Onde pode rodar futuramente
PRIORIDADE de arquitetura: monitor de metadados read-only em repositório de automação SEPARADO ou outra infraestrutura aprovada, sem ter acesso ao código privado DanPlus. Não criar repositório nem workflow agora. Se Github Actions: schedule só funciona a partir da branch default do repositório da automação; proteger GITHUB_TOKEN com contents:read. Diagnóstico profundo da base DanPlus só localmente no Windows ou em ambiente privado expressamente aprovado. Não reutilizar o repo legado de OTA como laboratório de automação.
Armazenamento de estado/report precisa ser explícito, não volátil (artifact GitHub sozinho não serve de ledger durável sem estratégia de retenção). Alertas apenas sobre diferenças reais ou falha do monitor. Definir canal de aviso e rotina depois do primeiro gate.

## 7. Aceites verificáveis (fixtures sintéticos)
- Nenhuma release em ambos => ZERO relatórios novos, ZERO builds, ZERO PRs.
- Uma nova no Original + uma nova no ARC => DOIS eventos vinculados, UM resumo consolidado, oportunidades sem duplicação.
- Duas releases entre dois polls na mesma origem => ambas registradas, sem perda.
- Feature já presente no DanPlus e nas duas fontes => JÁ PRESENTE, sem tarefa de código.
- Feature no Original e ausente no ARC => candidato separado, sem fingir que ARC já herdou.
- Feature somente ARC => tratar como delta fork, não rotular oficial.
- Tag republicada com SHA divergente => HOLD e alerta de integridade.
- API de uma fonte indisponível => PARCIAL/FAIL (não "sem atualizações") e recuperação sem perder ledger.
- Repetição, concorrência, rate limit => deduplicação idempotente e sem builds/PRs extras.
- Gate com RS-06, B2-06, 110 pendentes ou decisão INVESTIGAR => jamais gera patch aprovado.
- Agendamento => provar evento schedule em branch default e alerta heartbeat; teste manual isolado não é prova.
- Quando houver integração futura, nenhum teste incompleto/pulando/expirado será PASS.

## 8. Entrega e separação do Codex
Missão de automação **DP-ML11** completamente separada de DP-ML00/01 (P0 Detalhes), DP-ML09 (ponte OTA) e DP-ML08 (trailers). **Atualização 18/11: preferir implantação inicial sem Codex, usando ChatGPT+Commander+GitHub Actions; estudo canônico em DP_AUTOMACAO_GITHUB_SEM_CODEX_ESTUDO_20261118.md.** Arquivo para enviar ao Codex: `DP_CODEX_10_SCRIPT_ML11_MONITOR_DUAL_20261118.md`. Codex só recebe autorização para etapas que o proprietário escolher expressamente.
O Nuvio ARC possui releases independentes; não copiar o modelo de auto-merge/publicação do Reshaped. Antes de qualquer implementação conferir repositórios, licença, branch, status Git e decisões atuais.
