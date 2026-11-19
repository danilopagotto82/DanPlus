# SCRIPT CODEX — DANPLUS · DP-ML11 · MONITOR NUVIO ORIGINAL + ARC
Data: 18/11/2026. **PARA USO FUTURO, QUANDO ENVIADO EXPLICITAMENTE AO CODEX.** O armazenamento deste script não autoriza execução da missão.

## Ferramentas e meta
Codex com capacidade de implementação, raciocínio Alto para desenho/testes críticos. Windows DESKTOP-5P89BAD; usar PowerShell/Node e testes locais mecânicos. ChatGPT GPT-6 Alto coordena e decide escopo. Não usar Work nem chamadas IA contínuas para monitorar.
Criar arquitetura do monitor de releases OBRIGATORIAMENTE DUAL: Nuvio Original `NuvioMedia/NuvioTV` e Nuvio ARC `ysosrs123/NuvioTV-Fork`. O fork ARC é linhagem histórica, mas a única baseline funcional comparável é o código DanPlus atual confirmado. Reshaped é opcional e independente.

## Leia antes de tocar
1. `D:\APK\Organizacao DanPlus 2.0\DP_CODEX_00_LEIA_PRIMEIRO_20261117.md`;
2. `DP_CODEX_08_ESTUDO_AUTOMACAO_UPSTREAM_20261117.md` e correção mandatoria `DP_CODEX_09_MONITOR_DUPLO_ORIGINAL_ARC_20261118.md`;
3. `DP_CODEX_01_MEGALOTES_E_GATES_20261117.md`, `DP_CODEX_03_MATRIZ_146_E_RS_20261117.md`, `DP_CODEX_05_RISCOS_CONTINUIDADE_20261117.md`, MUD R3, AGENTS e Git/status atuais.
Se houver contradição, este roteiro/DP-CODEX-09 prevalece SOMENTE sobre a contagem e prioridade das fontes do monitor. As 36 decisões originais, RS e proteções de execução continuam soberanas.

## Escopo seguro de implementação futura por fases
**ML11-00 PRECHECK SOMENTE LEITURA:** confirmar contas/repositórios/release APIs, política de custos e CI, Git/status do computador, arquivos canônicos, onde pode residir o monitor, como persiste ledger, permissões e limites. Apresentar em PT-BR qual infra é viável. NÃO criar repositório, Actions, cron, issue, PR, workflow remoto ou alterar worktree protegida nesse gate. Se local seguro não existir, STOP e indicar bloqueio.
**ML11-01 PROTÓTIPO LOCAL ISOLADO (após gate e autorização próprios):** criar apenas arquivos do monitor em diretório/branch NOVO aprovado, sem tocar código Android. Implementar coletores independentes para os dois releases APIs, paginação, release+tag+SHA, tags alteradas, dedup, ETag, erro 403/429/5xx, persistência atômica e lock; mocks/fixtures para 10 cenários de DP-CODEX-09. Nenhuma atualização APK, merge/PR, deploy ou instalação.
**ML11-02 RELATÓRIO SEM IA (após teste local):** gerar resumo PT-BR, diffs com links, categorias e estado do catálogo, sem alterar decisões. Onde não houver acesso à baseline DanPlus privada, escrever COMPARAÇÃO COM DANPLUS NÃO TESTADA, nunca considerar integração verificada.
**ML11-03 AGENDAMENTO REAL (outro gate):** aprovar repositório/infra, branch default, segredos e permissões mínimas; executar manual e agendado, provar cron e alerta de ausência. Sem publicar ou liberar updates. Eventos podem ser verificados sem custo de IA recorrente.
**ML11-04 PR/CI CANDIDATO (gate futuro específico):** só depois de decisões humanas e reconciliação de P0, OTA, ABI Fire/Shield e regras de segurança. Não fazer neste lote inicial.

## Requisitos que não podem ser omitidos
- Não confundir ARC com Nuvio Original nem com Reshaped; registrar identidade por repo para evitar colisão de tags iguais.
- Não pegar apenas release mais recente; processar todas as novas em ordem e as duas fontes sem dependência entre si.
- Correlacionar sem duplicar melhorias quando uma mudança migra Original → ARC → DanPlus; sem provas, usar NÃO VERIFICADO.
- 146 IDs originais, 36 decisões/110 pendências, 7 RS/RS-06 pendente, B2-06 pendente; preservar 14 MUD R3 e BUG-THUMB-01.
- Não armazenar tokens, código privado ou segredos em relatório GitHub público; não ler `.wrangler/`.
- Nada de `git clean`, `reset --hard`, `stash` geral, força Git ou mudança em repo legado. Ponte ML-C1 está suja e NÃO deve ser tocada.
- Não instalar nas TVs, não compilar APK, não mexer em OTA/Supabase/keystore/main, não publicar releases nem criar repositório público/privado sem novo gate.

## Evidências/aceite e encerramento
Antes de marcar PASS: rodar testes locais explícitos para original-only, ARC-only, ambos novos, ausência, duas novas no intervalo, duplicidade semântica, tag mutada, erro parcial de API, concorrência e HOLD humano. Demonstrar execução repetida idempotente e nenhum build/merge/push. Se não houver execução, marcar NÃO TESTADO.
Entregar `FEITO / RESULTADO / PRÓXIMO PASSO` em português simples, com arquivos, branch, SHA, resultados reais PASS/FAIL/NÃO TESTADO, consumo/custo verificável se houver, rollback e checkpoint fora de produção. Parar ao fim da fase explicitamente autorizada; não avançar para scheduling ou escrita remota por iniciativa própria.

## Prioridade e independência
Este script NÃO substitui o de implementação principal `DP_CODEX_06_SCRIPT_INICIO_20261117.md`, que começa por DP-ML00/01 para estabilizar Detalhes. Não executar os dois pipelines com escritores concorrentes; o monitor read-only pode ser planejado em paralelo, mas não compete por build Android.
