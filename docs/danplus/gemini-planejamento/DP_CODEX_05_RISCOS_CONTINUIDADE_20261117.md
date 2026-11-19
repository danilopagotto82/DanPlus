# DANPLUS · RISCOS, BLOQUEIOS E CHECKPOINT DE CONTINUIDADE
Data 17/11/2026. Prontidão documental; nenhuma implementação/banco/APK alterados.

## Evidências verificadas nesta preparação
- Commander Principal DESKTOP-5P89BAD online; worktree original HEAD 0ce90a0 branch danplus/2.0-ml01-integracao, somente ?? .wrangler/ pré-existente.
- Worktree OTA independente HEAD 99f740c branch danplus/mlc1-ponte-ota-20261117, com alterações NÃO COMMITADAS em app/build.gradle.kts, UpdateRepository.kt, AbiSelector.kt, ForkUpdateRepositoryTest.kt e diretório novo app/src/testFullFire/. **Documento histórico da ponte reportava só três alterações: estado real ATUAL prevalece; não sobrescrever.**
- Registros de decisão do PC relidos: original 36/146, 110 pendentes; Reshaped 6/7, RS-06 pendente. B2-06 ausente das decisões (PENDENTE).
- Ponte OTA histórica: 14 testes PASS, build Fire PASS de LAB, build Shield FAIL por arquivo nuviotv.jks não encontrado naquela worktree. Nenhum smoke físico de atualização autorizado/registrado.
- P0 relatado: sete crashes distintos Shield e quatro Fire, trace de Detalhes em 7/7 envios; assinatura Compose; concorrência scroll/foco é HIPÓTESE e deve ser testada.
- Screenshot do usuário e código de SeekThumbnailPane: BUG-THUMB-01 mostra possível recorte interno ContentScale.Crop / aspect fixo 16:9 no Seekr / ThumbCrop e clipToBounds/âncora. **Não inferir causa única apenas da foto.**
- R3 original e evidências 2.0.0: maioria das 14 MUDs codificadas e 120/120 testes JUnit históricos PASS; nenhuma validação de nova versão feita nesta consolidação.

## Tabela de riscos com decisão preventiva
| Risco | Evidência | Decisão/limite |
|---|---|---|
| Dados/progresso perdidos | várias sincronizações por perfil e addons | backup antes de patch, fixtures sintéticas, rollback, jamais limpar DB/perfis |
| Crash Compose intermitente | último Detalhes, evento layout duplo | ML01 isolado, reproduzir/instrumentar, não importar fix da Home presumindo solução |
| ROM/virtual memory Fire 32 bits | A1-23/A2-27 e múltiplos geradores | um consumidor pesado por vez, memória/CPU real antes de alterar |
| Carregamento de fonte sem fim | B1-27 ampliado até exaustão | orçamento global, uma tentativa ativa, lista elegível, cancelamento; rede lenta != fonte morta |
| Legendas externas com retiming incorreto | RS-01/VAD, RS-02/ASR, regional PT | opt-in desligado, preservar áudio/ASS/MPV, pt-BR != pt-PT, sem modelo automático |
| Rede consumida pelo sincronizador | Reshaped pode amostrar 150–400MB de tráfego na release estudada | sem amostragem alta automática; medir antes de definir cotas, proteger Fire |
| Player avançado/HDR/HD audio | MPV, Exo, Dolby/DTS/Atmos/DV | patches em áreas pequenas, QA Shield/Fire, não trocar codec por hipótese |
| OTA quebra atualização | repos antigo/novo e nomes APK Fire, versionCode 1642 | ML09 isolado, bridge old/new, signer idêntico, instalação e publicação separadas |
| Segredos/alertas GitHub | dois alertas API antigos, .wrangler, keystore ausente | não imprimir/rotacionar/copiar, tratamento de segurança independente |
| Supabase e cross-profile | MUD-002/006/011/012, novos caches trailer | SQL e contratos sintéticos, RLS/ownership/rollback, deployment só após gate |
| Conflitos com outro agente | bridge dirty e outros projetos Android | conferir processos/worktrees, não fazer reset/clean/force/kill, escritor único |
| Repetição de MUD | código 2.0 já contém funções originais | confirmar baseline e mudança necessária antes do patch; não repetir menus/engines |
| Escopo não aprovado | 110 pendentes, RS-06, B2-06 e A2-35 | preservar registro, nunca implementar sem decisão nova; investigação ≠ patch |
| Perda de contexto | projeto longo, 14 MUDs e múltiplos forks | checkpoint após cada lote, IDs, SHA, arquivos, testes, evidência e próximo passo |

## Integração e reconciliacão, sem retrabalho
- Fluxos combináveis: B1-27 + B2-07 + B2-02 + RS-03 + A2-49 (um mecanismo de recuperação); A1-22 + A2-38 (um contrato histórico); MUD-013 + REQ16-04/07 + BUG-THUMB-01 (mesma superfície, camadas distintas); MUD-006 + REQ16-06 (mesma tela); MUD-008 + REQ16-05 (mesma Focus Dock).
- Trilhas separadas: ML-C1 OTA 2.0.1; RS-01 áudio por fala; trailers YouTube+Supabase; backend/segredos e migrations. Não reunir estas com P0/DPAD no mesmo patch.
- As 36 decisões P−1 incluem 4 MANTER, vários INVESTIGAR e A2-35 NÃO RECOMENDAR. Nada autoriza copiar 36 patches indiscriminadamente.
- RS-04 MANTER engine existente NÃO proíbe corrigir o BUG-THUMB-01: corrigir enquadramento sem trocar gerador. RS-07 INVESTIGAR não requer nova API de notas.

## Política de recuperação
- Antes de modificar, snapshot Git/worktree de desenvolvimento criado e testado de forma não destrutiva. Usar branch/worktree isolada, registrar HEAD/TREE, diffs, sha256 de artefatos, arquivos staged/untracked e status exato.
- Jamais reverter automaticamente arquivos de usuário. Se build falhar, consertar primeiro SDK/JDK/Gradle/keystore/ABI/portas, sem desativar UAC/Defender/SmartScreen.
- Se houver sinal fundamentado de perda de contexto: “⚠️ Melhor iniciar um novo chat. Esta conversa apresenta risco de perda de contexto. Copie o script abaixo.” e salvar CHECKPOINT_CONTINUIDADE no diretório de Organização com projeto, escopo, branch, decisões, patch, testes, riscos, próximos gates.
- Modelo: GPT-6 Alto coordena; Codex Alto codifica risco; terminal mecânico; sem agente caro aguardando compilação automática.

## Estado de entrega desta preparação
PASS: precheck e leitura de documentos identificados, matriz 146/RS7, escrita dos documentos organizacionais e revisão de status Git. NÃO EXECUTADO: patch, novos testes/gradle, builds, screenshot do app real após patch, ADB smoke, integração, merge, criação de repo, OTA, release ou deploy.
Próxima ação exata: enviar DP_CODEX_06_SCRIPT_INICIO_20261117.md ao Codex com acesso ao computador D:, iniciar ML00 read-only e depois ML01 isolado se os gates PASS.