# DANPLUS · PACOTE MESTRE DE EXECUÇÃO — LEIA PRIMEIRO
Emissão: 17/11/2026 — consolidação de especificações; **NENHUMA CODIFICAÇÃO NESTA RODADA**.

## Objetivo e convenções
Preparar o Codex para corrigir a estabilidade do DanPlus 2.0, validar as funcionalidades entregues, implementar apenas diferenças efetivamente necessárias nas decisões ADAPTAR e preservar integralmente Shield, Fire e OTA. **Não prometer zero falhas; obter evidências reais e parar em gates falhos.**

## Estado factual e prioridades
- PC: DESKTOP-5P89BAD. Base de código PROTEGIDA D:\APK\Worktrees\DanPlus_2_0_Unificado_ML01_20261115; branch danplus/2.0-ml01-integracao; HEAD 0ce90a0740656c903b9780d2d423966b15119697; git status só ?? .wrangler/ pré-existente; NUNCA acessar ou limpar .wrangler.
- 2.0.0 entregue em duas variantes (Shield ARM64 com.danplus.shield e Fire ARMv7 com.danexus.tv.fire.debug), versionCode 1642. Relato documental de 120/120 JUnit da entrega; NÃO implica validação física.
- P0 atual: crash de Compose "Place was called on a node which was placed already". Relatório documenta sete eventos distintos Shield + quatro Fire; 7/7 últimos históricos de tela terminam em Detalhes, mas a causa exata segue NÃO COMPROVADA. Home desaparecendo e outras falhas são relatos distintos.
- 14 mudanças MUD R3 da 2.0 já estão em grande parte codificadas; verificar antes de tocar. Backends MUD-006/012 estão prontos localmente mas migração remota não executada; MUD-011 sincronização de credenciais bloqueada por segurança/termos do provedor.
- Auditoria Nuvio/Arc: 146 IDs; 36 decisões de planejamento, 110 pendentes. Reshaped: sete IDs; seis decisões, RS-06 pendente. B2-06 pendente. BUG-THUMB-01 registra corte visual da prévia, não autoriza trocar motor.
- **Versão 2.0.1** foi escolhida para a futura ponte OTA; nome exibido do aplicativo será **DanPlus** (sem sufixo 2.0), preservando IDs e certificados. Repositório futuro planejado danilopagotto82/DanPlus, repositório antigo intacto; NÃO criar/renomear/publicar sem gate.
- **Ponte OTA ML-C1 é trilha paralela, não baseline limpa para novos lotes**: worktree D:\APK\Worktrees\DanPlus_PONTE_OTA_MLC1_20261117, branch danplus/mlc1-ponte-ota-20261117, HEAD 99f740cd63e73cb8f5f3b6a51180a4bd001b4d7e. Inspeção recente mostrou 4 arquivos rastreados MODIFICADOS e diretório app/src/testFullFire/ NÃO RASTREADO; preservar tudo. Relatório histórico registra 14 testes PASS, build Fire de laboratório PASS, Shield build FAIL por keystore ausente; diferenças atuais não foram revalidadas.
- A branch central de documentação foi integrada no commit 99f740c. **O Git remoto da base protegida é um repositório local, não presumir origin GitHub para operações remotas.** Codex deve descobrir a worktree limpa e o ancestry antes de criar seu próprio isolamento.
- Os documentos criados aqui ficam somente em D:\APK\Organizacao DanPlus 2.0, FORA do Git do aplicativo. Nenhum checkpoint de código, branch, instalação, build ou GitHub novo foi criado nesta preparação.

## Ordem de leitura obrigatória do Codex
1. Este arquivo e DP_CODEX_01_MEGALOTES_E_GATES_20261117.md.
2. DP_CODEX_02_PRECHECK_ACEITE_E_PROTECOES_20261117.md; DP_CODEX_03_MATRIZ_146_E_RS_20261117.md; DP_CODEX_04_MUD_R3_PEDIDOS_PENDENTES_20261117.md.
3. DP_CODEX_05_RISCOS_CONTINUIDADE_20261117.md e DP_CODEX_06_SCRIPT_INICIO_20261117.md.
4. Código atual, Git status/worktrees e AGENTS.md da raiz D:\APK (não existe AGENTS.md na raiz da worktree protegida nesta inspeção).
5. INICIAR_AUDITORIA.md; D:\APK\Evidencias\DANPLUS_2_0_ML00_R3_20261115\01_DANPLUS_2_0_MUD_MASTER.md e as 14 pastas DECISAO.md/MISSAO_IA.json/SCRIPT_EXECUCAO.md; checkpoint codificação R3 e evidências da 2.0.0.
6. P0_CONCLUSAO_CRASHES_COMPOSE_20261117.md, P1_HOME_FOCO_ETAPA_20261117.md; demais relatórios P−1 por ID.
7. MISSAO_CODEX_MLC1_PONTE_OTA_20261117.md e RELATORIO_MLC1_PONTE_OTA_20261117.md SÓ para a trilha OTA, sem reaplicar patch em outra branch.
8. Evidência visual do BUG-THUMB-01: foto enviada no chat anterior; o pacote de handoff inclui screenshot como anexo quando fornecido ao executor.

## Regra de precedência
Código/runtime e testes atuais > Git > 14 MUDs R3 oficiais e decisões expressas recentes > relatórios e histórico. Status de documento antigo não prevalece automaticamente sobre o registro oficial de 36/146 e 6/7. Hipótese nunca vira causa comprovada. Precheck de merge PR #1 não autoriza reexecutá-lo.

## Regras de execução
- ChatGPT normal GPT-6 Alto é coordenador; **Codex** implementa somente quando o arquivo de início for enviado explicitamente em sessão com acesso ao Windows. Commander/PowerShell/Gradle executam tarefas mecânicas; sem Work ou agente caro para polling.
- Cada megalote: PRECHECK → checkpoint → plano/escopo → patch mínimo → testes unitários/integração focados → build local autorizado → screenshot/smoke quando permitido → diff/segredos → PASS/FAIL/NÃO TESTADO.
- **Não mover nem alterar** Shield/Fire antigos, main, OTA, bibliotecas internas, Supabase remoto, README público/repo GitHub ou credenciais por iniciativa própria.
- Um executor Android e um build pesado por vez; checar processos e trabalhos concorrentes. Sem git clean/reset hard/stash geral, force push, releases, OTA, instalação, migrations, rotação de chaves ou pagamento sem decisão específica.
- **Primeiro trabalho indicado:** DP-ML00 e, após seus gates, DP-ML01 (diagnosticar e corrigir P0 Detalhes em worktree NOVA isolada). Os demais lotes ficam ordenados, NÃO devem ser aplicados todos de uma vez.

## Índice de entrega deste pacote
DP_CODEX_00_LEIA_PRIMEIRO_20261117.md — visão geral.
DP_CODEX_01_MEGALOTES_E_GATES_20261117.md — lotes e dependências.
DP_CODEX_02_PRECHECK_ACEITE_E_PROTECOES_20261117.md — barreiras e testes.
DP_CODEX_03_MATRIZ_146_E_RS_20261117.md — todos IDs e decisões.
DP_CODEX_04_MUD_R3_PEDIDOS_PENDENTES_20261117.md — mudanças originais e novos pedidos.
DP_CODEX_05_RISCOS_CONTINUIDADE_20261117.md — riscos, o que não fazer, checkpoint.
DP_CODEX_06_SCRIPT_INICIO_20261117.md — prompt para colar no Codex.
DP_CODEX_07_CHECKPOINT_CONTINUIDADE_20261117.md — checkpoint de continuidade.
DP_CODEX_08_ESTUDO_AUTOMACAO_UPSTREAM_20261117.md — estudo aprofundado, futura missão DP-ML11 (não executada); atualizado para DUAS FONTES obrigatórias em 18/11.
DP_CODEX_09_MONITOR_DUPLO_ORIGINAL_ARC_20261118.md — arquitetura corrigida: Nuvio Original + Nuvio ARC, comparação contra DanPlus.
DP_CODEX_10_SCRIPT_ML11_MONITOR_DUAL_20261118.md — roteiro ALTERNATIVO para Codex, somente se realmente necessário após gate.
DP_AUTOMACAO_GITHUB_SEM_CODEX_ESTUDO_20261118.md — NOVA ROTA PREFERIDA: GitHub Actions + análise/CI/revisor de PR, sem Codex recorrente (apenas estudo, não ativado).
Documentos canônicos originais NÃO foram reescritos.

**STATUS desta consolidação:** organização concluída; implementação e novos testes de APK/build/smoke NÃO EXECUTADOS.
