# DANPLUS · PROTOCOLO DE SEGURANÇA / TESTES / ACEITE DO CODEX
Data 17/11/2026. Sem execução de código neste documento.

## PRECHECK obrigatório ANTES de todo megalote
1. Confirmar PC DESKTOP-5P89BAD, leitura de D:\APK\AGENTS.md e da documentação prioritária, worktree correta, branch, HEAD, status, refs e executores concorrentes. Nunca presumir que o histórico continua atual.
2. Fonte homologada PROTEGIDA: D:\APK\Worktrees\DanPlus_2_0_Unificado_ML01_20261115 HEAD 0ce90a0740656c903b9780d2d423966b15119697; entrou com ?? .wrangler/ pré-existente. **Não ler nem copiar .wrangler**, não usar git clean/reset/stash.
3. Antes de CODIFICAR, selecionar base de desenvolvimento limpa a partir da central 99f740cd63e73cb8f5f3b6a51180a4bd001b4d7e **somente se** ancestry/tree/estado atual confirmarem que é a mesma 2.0.0 com atualização documental; criar worktree NOVA com branch única identificada pelo ML, sem alterar a original. Alternativa conservadora: clonar estrutura em cofre existente se o Git não permitir criar worktree isolada, sem hardlinks do Git e sem mexer nos originais; registrar motivo.
4. Inspecionar todos os consumers e dependências do componente a editar, arquivos de build, variantes, bibliotecas, Gradle/JDK/SDK e testes existentes. Checar dados dos perfis e risco de segurança.
5. Se worktree escolhida estiver dirty, fazer checkpoint com nomes/hashes e **preservar todas as alterações**. NÃO sobrescrever arquivos de usuário. Se conflito, HOLD.
6. Um escritor Android ativo e um Gradle pesado por vez. Checar Codex/Antigravity/Commander e processos Java/Gradle; não encerrar execução de outro projeto para liberar CPU/RAM sem autorização.
7. Fazer leitura direcionada antes de alterar: MUD R3 + DECISAO.md, MISSAO_IA.json, SCRIPT_EXECUCAO.md de cada mudança original, imagem homologada MUD-008, decisões P−1/RS atuais, código real e testes. Registrar arquivos não lidos.
8. Não acessar credenciais, local.properties, local.dev.properties, API tokens, keystore/senhas ou .wrangler fora do mecanismo seguro necessário, nem registrar valor em documentação/log.

## Ações PERMITIDAS na sessão Codex futura (só após o usuário enviar o script)
- PRECHECK e ML00 em somente leitura e produção de checkpoint/documentos locais, preferindo as pastas da Organização e Evidências.
- Após PASS ML00, criar branch/worktree ISOLADA para ML01 e implementar correção pontual P0 Detalhes fundamentada em evidência; testes focados; build local de LAB quando viável, sem instalar ou publicar; commit LOCAL só após revisão e sem segredos.
- A autorização não é para aplicar os 146 itens, nem instalar/push/merge/release, nem modificar Supabase real. Os lotes posteriores dependem dos gates descritos e de revisão da integração; nenhum patch pode ser inferido de INVESTIGAR ou PENDENTE.

## Barreiras SEM NEGOCIAÇÃO
- NÃO alterar applicationId, package de instalação, assinatura/certificado, PIN/perfis/dados do usuário, nome do repo GitHub, URL OTA instalada, default branch, main/tag/release, arquivos de terceiros e licenças sem gate específico.
- NÃO desfazer alterações locais, copiar direto implementação do Arc/Reshaped ou sobrescrever player/Seekr atual. GPL-3.0, NOTICE e autoria precisam permanecer.
- NÃO publicar nem instalar APK, usar ADB para alterar aparelhos, fazer migrations reais sem rollback e gate, alterar auth/RLS/segredos em produção ou ativar API paga sem aprovação.
- B2-06 e RS-06 PENDENTES. A2-35 NÃO RECOMENDAR. Em MUD-011 Seekr/segredos, HOLD por termos e segurança. RS-02 ASR, somente INVESTIGAR.
- Ponte OTA ML-C1 separada está DIRTY (quatro arquivos alterados e diretório de teste não rastreado na última inspeção). Não executar reset/clean/checkout/merge nela nem usá-la como base limpa sem conciliar.

## Testes e aceites por produto
- **DP-ML01 Detalhes:** reproduzir falha ou criar instrumentação controlada, identificar conflito de scroll/foco com rastros limitados e redigidos, testes Kotlin e preferencialmente instrumentados. Falhas de Compose não podem reaparecer na sequência reproduzida. **Não declarar fix sem teste físico** quando exigido.
- **UX e BUG-THUMB-01:** screenshot real antes/depois, origem do thumbnail e comparação 16:9/outros aspectos; provar se houve crop interno versus clip externo; testar frame inferior completo, tempo/label e barra; Shield e Fire, DPAD lento/rápido e Classic/V2 quando aplicável.
- **BUG-BACKDROP-01 (ML02 + aceite funcional ML06, FUTURO):** testar o fundo de Indicações e o backdrop no foco de Descobrir sem a manobra de abrir Detalhes e voltar. Separar cache vazio/quente e rede lenta; registrar tempo foco→imagem exibida, troca rápida DPAD, alternância de perfil, imagens corretas sem fantasmas, fallback e recomposição automática quando a imagem chega. Verificar consumo de RAM/cache Fire 32 bits e Shield, screenshots antes/depois do aplicativo real. Não impor prefetch ilimitado nem afirmar causa/cache resolvido apenas porque o build passou. Especificação: BUG-BACKDROP-01 no DP_CODEX_04. Sem QA físico = NÃO TESTADO.
- **Playback e failover:** testar pelo menos sete fontes sintéticas (seis inválidas, sétima válida), zero fontes, timeout de addon, desistência/cancelamento, mudança de perfil/episódio, fonte manual e dead link. Nunca avançar para fonte de outro addon sem regra; nunca repetir eternamente. Fluxo atual B1-27 tem limite 3 trocas e é restringido a seleção automática ANTES do 1º frame: adaptação de meio de filme é escopo separado.
- **Fire:** validar ambiente 32-bit, memory address-space, cache/seek e buffer sob pressão sem OOM/ANR; playback e áudio intactos. Não extrapolar medições feitas na Shield.
- **Legendagem:** ExoPlayer/MPV, libass/ASS com estilo, seleção pt-BR vs pt-PT, áudio dublado e original, sem forçar download de 74 MB; limitar tráfego, preservar DTS/TrueHD passthrough.
- **Históricos:** Trakt/Simkl/MDBList, persistência por perfil, dados de watchlist, progress e opções existentes. Testar offline, sincronização e rollback sintético; sem apagar usuário real.
- **OTA 2.0.1:** pacote e certificado iguais aos instalados, versionCode > 1642 e > code instalado, variante Fire v7a e Shield arm64, nome visível DanPlus, old/new URLs allowlist e rollback; até smoke in-place autorizado é HOLD.
- **Backend:** RLS, ownership, isolamento entre contas, migration reversible em DB sintético; deploy real somente após permissão.

## Contrato PASS/FAIL
- PRECHECK / PATCH / TESTE FOCADO / BUILD / SMOKE / REVISÃO DO DIFF / ROLLBACK: cada um recebe PASS, FAIL ou NÃO TESTADO com comandos, outputs, SHA e caminhos.
- Componente compilado não equivale à usabilidade aprovada. Unit test aprovado não valida imagem. Teste ausente, interrompido, expirado ou baseado em relatório antigo NÃO equivale a PASS atual.
- Registrar screenshot/reprodução com origem e data; excluir dados pessoais e tokens de logs. Comparar baseline física na MESMA TV e modo de navegação.
- Ao detectar FAIL não ampliar automaticamente escopo. Voltar ao patch mínimo e isolar causa; não prometer estabilidade absoluta.
- Publicação só após gates separados: versão/signing → build/hashes → smoke in-place Shield/Fire → aprovação do usuário → release. Deploy remoto e renomeação GitHub têm gates distintos.

## Resultado da preparação
Apenas inspeção do Git e escrita destes .md de planejamento no diretório de Organização: PASS. Build, código, smoke e teste novo NÃO EXECUTADOS.