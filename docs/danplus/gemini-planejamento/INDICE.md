# DanPlus — Índice das próximas mudanças para Gemini CLI

**Cópia fiel de 17 arquivos MD**, capturada de `D:\APK\Organizacao DanPlus 2.0` em 18/11/2026. Cada cópia foi verificada por SHA256. Os originais permanecem no local anterior. **Este é um snapshot de planejamento — não são funcionalidades implementadas nem autorização de execução.**

## Ordem de leitura obrigatória

1. `DP_CODEX_00_LEIA_PRIMEIRO_20261117.md`: contexto, regras e estado histórico.
2. `DP_CODEX_01_MEGALOTES_E_GATES_20261117.md`: DP-ML00..11, dependências, aceite e BUG-BACKDROP-01.
3. `DP_CODEX_02_PRECHECK_ACEITE_E_PROTECOES_20261117.md`: precheck, proteção, QA e PASS/FAIL.
4. `DP_CODEX_03_MATRIZ_146_E_RS_20261117.md`: 146 itens, 36 decisões, 110 pendentes; Reshaped sete itens, seis decisões.
5. `DP_CODEX_04_MUD_R3_PEDIDOS_PENDENTES_20261117.md`: MUD R3, REQ16 (14 pedidos), BUG-BACKDROP-01.
6. `DP_CODEX_05_RISCOS_CONTINUIDADE_20261117.md`: riscos e anti-retrabalho.
7. `DP_CODEX_06_SCRIPT_INICIO_20261117.md`: roteiro para Codex em futura missão DP-ML00/01, **NÃO executar agora**.
8. `DP_CODEX_07_CHECKPOINT_CONTINUIDADE_20261117.md`: checkpoint histórico, confrontar com o código atual.

## Monitor de releases e automação (DP-ML11, apenas estudo)

9. `DP_CODEX_08_ESTUDO_AUTOMACAO_UPSTREAM_20261117.md`: arquitetura de automação.
10. `DP_CODEX_09_MONITOR_DUPLO_ORIGINAL_ARC_20261118.md`: **Nuvio Original + Nuvio ARC** obrigatórios; Reshaped opcional.
11. `DP_CODEX_10_SCRIPT_ML11_MONITOR_DUAL_20261118.md`: plano alternativo via Codex, somente mediante gate.
12. `DP_AUTOMACAO_GITHUB_SEM_CODEX_ESTUDO_20261118.md`: rota preferida GitHub Actions sem IA recorrente; **não implantada**.

## Decisões e correções específicas

13. `DECISOES_OFICIAIS_P1_20261117.md`: decisões oficiais Original/ARC.
14. `DECISOES_OFICIAIS_RESHAPED_P1_20261117.md`: decisões Reshaped.
15. `BUG_THUMB_01_CORTE_INFERIOR_MOLDURA_20261117.md`: miniatura Seekr cortada, preservar motor atual.
16. `P1_RESHAPED_ETAPA2_PREVIEWS_FONTES_NOTAS_20261117.md`: previews, fontes e avaliações.
17. `P1_B127_FAILOVER_INTELIGENTE_20261117.md`: recuperação de fontes do mesmo addon sem looping.

## Correções de contexto posteriores aos MDs

- A **ponte OTA 2.0.1** não é a baseline limpa para mudanças. Avatares Fire: PASS físico. Filme completo e áudio na soundbar: **PASS por relato direto do usuário em 18/11/2026**. Capturas ADB e MediaSession anteriores não representaram confiavelmente a experiência real.
- OTA online e publicação: **NÃO TESTADO / HOLD**. Nenhuma instalação, merge, release ou renomeação autorizada.
- Histórico Git local está **shallow** com remotes `fonte-local` e `github-publico`; remotes Original/ARC ainda não configurados. Não forçar reconstrução de ancestry.
- `.gitignore` atual protege `local.properties` e `.env`, mas faltam regras genéricas para `*.jks`, `*.keystore` e `keystore.properties`. Corrigir e auditar antes de futuro push público, sem jamais copiar segredos.
- Prioridades: DP-ML00 precheck → DP-ML01 P0 Detalhes → demais lotes por componente após gate. **BUG-BACKDROP-01** pertence à correção única de ML02 e validação específica ML06; **LOGO-PTBR-01** exige prioridade de logos TMDB em português brasileiro no Focus Dock Descobrir/Biblioteca (REQ16-05/ML02), com fallback apenas para idioma original da obra; **BUG-THUMB-01** é distinto. Trailers+cache Supabase são missão ML08 separada. Monitor Original/ARC é missão ML11 opcional.
- Status/decisões só se tornam atuais após confronto com código, Git e testes reais. Não confundir documentação, intenção ou teste antigo com funcionalidades aprovadas.

## Para começar no Gemini CLI

Ler `GEMINI.md` da raiz e este índice, depois o documento do megalote desejado. Consultar `D:\APK\AGENTS.md` e documentos originais se disponíveis. **Não implementar em lote, não editar a ponte OTA dirty, não ativar workflows, não alterar produção nem publicar.** Confirmar a missão e a baseline antes de qualquer patch.

### Correção do status histórico — 19/11/2026
As menções anteriores a `.gitignore` sem cobertura de assinatura ou remotes Original/ARC ausentes refletem o diagnóstico de 18/11. O commit local `3269f9b` já acrescentou as exclusões, e os remotes Original/ARC foram cadastrados para consulta sem push. Isso NÃO autoriza publicação; alertas de segurança e testes de migração OTA permanecem como gates.
