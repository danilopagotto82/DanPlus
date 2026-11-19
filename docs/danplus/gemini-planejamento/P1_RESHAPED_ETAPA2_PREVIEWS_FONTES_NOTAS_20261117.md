# DANPLUS P−1 — RESHAPED · ETAPA 2: miniaturas, fontes, avaliações
Data da análise: 17/11/2026. **Apenas investigação/documentação.** As sugestões abaixo NÃO são decisões humanas.

## Contexto preservado
- Catálogo próprio `DECISOES_OFICIAIS_RESHAPED_P1_20261117.md`: 6 decididas de 7 (RS-01 ADAPTAR, RS-02 INVESTIGAR, RS-03 ADAPTAR, RS-04 MANTER, RS-05 ADAPTAR, RS-07 INVESTIGAR); só RS-06 pendente.
- Catálogo original `DECISOES_OFICIAIS_P1_20261117.md`: **36/146** decididas, **110 pendentes**, B2-06 **PENDENTE**. Não misturar nem duplicar IDs.
- Código protegido: `D:\APK\Worktrees\DanPlus_2_0_Unificado_ML01_20261115`, branch `danplus/2.0-ml01-integracao`, HEAD `0ce90a0740656c903b9780d2d423966b15119697`, único não rastreado pré-existente `?? .wrangler/`; não tocar.

## Achados de comparação estática, aprofundados
| ID | Resultado REAL da comparação | Recomendo (pendente de dono) |
|---|---|---|
| **RS-04** | **DanPlus já tem geração local de miniaturas**, além de Seekr: `core/player/thumbnail/SeekThumbnailEngine.kt` e dezenas de módulos `MemoryGovernor.kt`, `ForegroundGate.kt`, `NextEpisodeThumbs.kt` etc. O Reshaped também tem engine portado do Nuvio Arc, conforme `seekpreview/engine/ARCHITECTURE.md`; substituição integral causaria risco de duplicar o mesmo trabalho. Diferenças finas de controle de memória/cobertura ainda não comparadas funcionalmente. | **MANTER** engine do DanPlus; investigar APENAS melhorias isoladas que resolvam bug comprovado; não portar a engine inteira |
| **RS-05** | DanPlus já **marca visualmente** a fonte atual no `StreamSourcesSidePanel.kt` via `findCurrentStreamIndex`, porém o foco inicial tende ao **primeiro item**. Reshaped `StreamSourcesSidePanel.kt` calcula `currentStreamKey` e foca a fonte que está reproduzindo, com controles de foco. O painel de legendas do DanPlus já tem `remember` e processamento em abertura, enquanto tester Reshaped anuncia otimização da reconstrução do menu. Não foi medido gargalo real. | **ADAPTAR** somente foco/seleção inicial da fonte em reprodução, sem alterar filtros/ordem/dados; **INVESTIGAR** subparte de menus/idiomas antes de programar |
| **RS-07** | DanPlus **já tem notas** MDBList/IMDb, `MdbListRatingsLoader.kt`, cache, componente `EpisodeRatingsSection.kt`, `ImdbRatingSourceLabel.kt` e integração Simkl/Scout. Tester Reshaped `1.2.0-beta.2-tester.4` acrescenta fonte de avaliação, notas por título/episódio e seletor. A paridade da **escolha da origem de notas** e compatibilidade com perfis não foi confirmada. | **INVESTIGAR** diferencial no seletor de origem e nos dados Simkl; NÃO duplicar client e tela de ratings |

## Fontes e regras de evidência
- Fonte do Reshaped (tag de teste com avaliação): https://github.com/DavidVamaiotu/NuvioTV-Reshaped/releases/tag/1.2.0-beta.2-tester.4
- Fonte Reshaped AutoSync: https://github.com/DavidVamaiotu/NuvioTV-Reshaped/releases/tag/1.2.0-beta.2-autosync.5
- Reshaped: `StreamSourcesSidePanel.kt`, `SubtitleSelectionOverlay.kt`, `seekpreview/engine/ARCHITECTURE.md`, `data/mdblist/MdbListRatingsLoader.kt`, `data/simkl/SimklRatings.kt`, `ui/screens/detail/EpisodeRatingsSection.kt` lidos no GitHub.
- DanPlus: inspeção por Git de arquivos equivalentes. **Comparação é estática:** ainda não houve avaliação prática do comportamento, imagem ou fluidez.

## Regras de preservação e testes futuros
- RS-04 é **redundante como port completo**. Preservar sistema de miniaturas local, Seekr, limites de cache, memória da Fire e CPU da Shield. Compatibilidade de versões não está comprovada.
- RS-05: foco na fonte correta é um ganho claro a validar; não refatorar o painel todo nem assumir que otimização de menu de legenda já falta. Validar fonte atual, troca manual, lista atualizada dinamicamente, teclado/DPAD.
- RS-07: manter MDBList, Simkl e Scout; evitar conflito na autoria de avaliações/sincronização. Distinguir notas públicas de notas dadas pelo usuário; não alterar configurações de perfis sem decisão.
- Os três foram decididos expressamente (RS-04 MANTER, RS-05 ADAPTAR, RS-07 INVESTIGAR). RS-06 permanece PENDENTE. A decisão RS-04 preserva o gerador, mas o defeito visual de enquadramento foi registrado separadamente em `BUG_THUMB_01_CORTE_INFERIOR_MOLDURA_20261117.md`; sem duplicar RS-04.
- Testes focados, build, smoke, ADB, uso físico, medição de rede, CPU e memória **NÃO EXECUTADOS**. Git, código, OTA, produção e aplicativos preservados.

## Atualização do registro e interrupção visual
- Em 17/11, decisões RS-04 MANTER, RS-05 ADAPTAR e RS-07 INVESTIGAR registradas e lidas de volta no catálogo oficial complementar.
- Screenshot de miniatura com parte inferior cortada motivou **BUG-THUMB-01**, auditoria estática do `SeekThumbnailPane.kt`, `ThumbCrop.kt` e ancoragem ao seek bar; causa exata NÃO TESTADA. Não confundir com autorização de mudar código.
