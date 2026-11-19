# SCRIPT AUTOSSUFICIENTE PARA COLAR NO CODEX — DANPLUS
Título da missão: DanPlus · DP-ML00 + DP-ML01 · Pré-check e correção P0 Detalhes
Ferramenta: Codex, modelo de código mais capaz disponível, raciocínio Alto. Ambiente Windows com acesso DIRETO ao PC DESKTOP-5P89BAD. Não usar Work por padrão. PT-BR, comunicação simples ao dono.

## Objetivo
Começar a implementação da próxima etapa DanPlus sem repetir mudanças da 2.0.0 nem quebrar OTA, perfis, player e Fire. **Nesta primeira missão, realizar ML00 e somente ML01 P0**. As melhorias ML02..ML10 estão planejadas e NÃO devem ser misturadas ou implementadas nesta missão. Trabalhar autonomamente nos passos locais reversíveis e testes após precheck, parando em gates sensíveis.

## Fontes e status reais
1. Abra D:\APK\Organizacao DanPlus 2.0\DP_CODEX_00_LEIA_PRIMEIRO_20261117.md e leia TODOS os arquivos DP_CODEX_01 até DP_CODEX_05 na mesma pasta, mais os originais vinculados (MUD MASTER R3, checkpoint pós-build, P0_CONCLUSAO_CRASHES_COMPOSE, AGENTS D:\APK e relatórios de tela Detalhes).
2. Fonte protegida D:\APK\Worktrees\DanPlus_2_0_Unificado_ML01_20261115 em danplus/2.0-ml01-integracao HEAD 0ce90a0740656c903b9780d2d423966b15119697 com ?? .wrangler/ pré-existente. NÃO ALTERE, LEIA, LIMPE OU COPIE .wrangler.
3. A central de documentação tem commit 99f740cd63e73cb8f5f3b6a51180a4bd001b4d7e após PR #1; só utilize como base de worktree nova se confirmar que o código é o mesmo da fonte homologada e que está em Git válido. Não deduza a partir de nomes. Leia o Git e os diffs. Não tocar main/OTA.
4. A ponte OTA está em worktree separada D:\APK\Worktrees\DanPlus_PONTE_OTA_MLC1_20261117, HEAD 99f740c, com arquivos locais modificados e app/src/testFullFire não rastreado. PRESERVE. Não usá-la para corrigir crash, não aplicar patches OTA agora.
5. A versão já entregue é 2.0.0/code1642, 2 APKs distintos; o próximo alvo OTA técnico escolhido é 2.0.1 e o nome visível só DanPlus, mas esse é OUTRO megalote.
6. Catálogos P−1 e RS: 36/146 e 6/7 decisões; 110 originais sem decisão, RS-06 e B2-06 pendentes. NENHUMA destas 110 pode ser implementada. RS-01 ADAPTAR é futuro; RS-02 INVESTIGAR. A2-35 NÃO RECOMENDAR. BUG-THUMB-01 será tratado depois em ML02, sem refazer Seekr.

## Registro novo a preservar para megalotes FUTUROS — 18/11/2026
O proprietário confirmou **BUG-BACKDROP-01**: backdrops de Indicações e de Descobrir demoram ou não aparecem no foco; hoje, muitas vezes só aparecem após entrar em Detalhes e voltar. **Requisito de produto:** carregar e atualizar imagens corretas sem essa manobra, com cache eficiente e DPAD fluido. Consulte a especificação integral em DP_CODEX_04_MUD_R3_PEDIDOS_PENDENTES_20261117.md e o encaixe nos megalotes em DP_CODEX_01_MEGALOTES_E_GATES_20261117.md.
**Nesta missão DP-ML00/01:** apenas registrar/mapear em leitura, se pertinente ao precheck; NÃO codificar BUG-BACKDROP-01 nem misturá-lo com P0 Detalhes. Após estabilização e novo gate, a implementação central pertence ao **DP-ML02**; a comprovação específica em Indicações ao **DP-ML06**. Não presumir que A2-32 (INVESTIGAR), pré-carregamento, CDN ou cache são a causa. Nenhuma medição ou correção ocorreu na inclusão documental.

## ML00 — obrigatório, somente leitura
- Confirme AGENTS e docs R3, código/branch/HEAD/status, worktrees, processos e executores concorrentes, package ids, assinaturas/gradle. Identifique a worktree limpa e uma branch nova e exclusiva (prefixo danplus/dp-ml01-p0-details, com sufixo único e sem colisão).
- Releia registros reais: P0_CONCLUSAO_CRASHES_COMPOSE_20261117.md relata 7 Shield + 4 Fire eventos distintos no DanPlus 2.0.0; nas 7 entradas Supabase a última rota é Detalhes, exception Place was called on a node which was placed already. Causa exata NÃO comprovada; scroll concorrente, lazy list e DPAD são suspeitos.
- Faça mapa preciso dos caminhos MetaDetailsScreen.kt, EpisodesSection.kt, DetailRowWindowAnchor.kt, requestScrollToItem, onPreviewKeyEvent e dpadVerticalFastScroll; veja testes existentes e os casos faltantes.
- Salve checklist e decisões de patch fora da worktree, sem segredos. Se precheck falhar, HOLD com causa e recuperação; não invada fonte protegida.

## ML01 — somente após ML00 PASS
- Crie worktree nova isolada (não a da ponte OTA), checkpoint seguro, branch exclusiva. Reproduza o bug por teste/instrumentação mínima ou isole conflito demonstrável com evidências. Se não conseguir reproduzir, não invente causa nem marque correção PASS.
- Corrija exclusivamente o defeito comprovado de scroll/foco/lazy list da tela Detalhes com patch mínimo e testes. Não refatore navegação inteira, não copie Arc2 Home como conserto presumido e não altere player, backend, assinatura, Seekr, menus da Fire/Shield ou histórico.
- Escreva testes focados para rolagem rápida, foco DPAD, recálculo/reordenação de itens, mudança de temporada, entrada/saída da tela; testes unitários e instrumentados quando ambiente permitir. Compile/teste local segundo Gradle wrapper real; respeite um build pesado por vez.
- Não instalar nas TVs sem gate específico. Se o QA físico for indispensável, registre NÃO TESTADO e organize comando de teste para autorização posterior; não declarar que patch resolve crash apenas porque compilou.
- Releia git diff --check, teste de regressão e segurança, e salve evidências + SHA na Organização/Evidências. Commit local apenas se escopo conferido, testes PASS e nenhuma credencial. Sem push/PR/merge/tag/release/OTA.
- Após ML01: entregue FEITO / RESULTADO PASS-FAIL-NÃO TESTADO / PRÓXIMO PASSO. Registre arquivo por arquivo, testes reais, build, crash reprodutível ou não, limites, rollback, divergências, e checkpoint autocontido. Pare para revisão antes de iniciar ML02.

## Proibições de segurança
NÃO fazer git reset --hard, git clean, stash geral, force-push; não renomear repo, publicar ou criar GitHub novo; não modificar production Supabase/migrations sem rollback/gate; não mexer em keystore/PIN/chaves, assinatura, applicationId; não alterar TVs via ADB ou instalar APK; não prometer desempenho. Não desativar antivírus/firewall/UAC. Não executar dois escritores Android nem dois builds pesados ao mesmo tempo. Preserve o USB de backup, projetos independentes e todas as alterações do usuário.

## Princípios finais
1. Primeiro estado REAL → Git → docs → memória.
2. O que já funciona NÃO deve ser refeito. Patch mínimo e rollback.
3. Hipótese NÃO é causa comprovada; teste ausente NÃO é PASS.
4. Não pergunte autorização repetida para passos locais reversíveis, mas PARE nos gates sensíveis indicados.
5. Nunca alegue ação que não executou. Relatar em PT-BR simples.
6. Se perder contexto, salvar CHECKPOINT_CONTINUIDADE_DANPLUS_DP-ML01.md em D:\APK\Organizacao DanPlus 2.0 com estado, arquivos, SHAs, testes PASS/FAIL/NÃO TESTADO e próxima ação; preservar trabalho.
COMECE AGORA pelo ML00 em leitura; se PASS, avance para ML01 local e isolado. Sem publicação ou instalação.