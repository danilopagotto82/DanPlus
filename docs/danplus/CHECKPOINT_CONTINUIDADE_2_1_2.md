# CHECKPOINT CONTINUIDADE 2.1.2

## ML01 — Design premium

- Branch `feat/danplus-2.1.2-homologacao-20261127`. Base `c27fbff4be8555aa6aa2e08b94cdfd8add0b62b3` e árvore `ea8e918e490a6b1ff9f56fdef884bc3206faa5e9` conferidas.
- Commit de implementação `529314c274b588c1cd2e6a64800892c0cead3f9f`. Árvore de implementação `276bf9183044686bdf9a1014b3306015a74f236b`.
- Teste focal `:app:testFullDebugUnitTest` em execução offline. Resultado final de JUnit — 111 PASS, 0 FAIL, 0 SKIP em 15 classes. XML em `app/build/test-results/testFullDebugUnitTest/`.
- `git diff --check` passou. A lista de tarefas foi conferida antes dos testes. O ML01 não gerou APK, não instalou, não publicou e não iniciou o ML02.
- ADB confirmou Shield com DanPlus 2.1.1/1646. AndroidTest e execução do candidato em TV continuam pendentes, assim como comparações antes/depois, DPAD físico, 1080p/4K, tempo de resposta, frames e RAM.

## Disposição por ID

- **UI-03 · PASS_TESTE_FOCADO.** Mesma faixa de 29% e grade. Altura medida depois de reservar metadados e créditos, até três retratos, criação e direção separadas, logo PT-BR/original com cache e cancelamento fora do foco. Testes — FolderPreviewCreditsTest; TmdbMetadataServiceTest; TmdbCollectionSourceResolverTest; GenreFoldersTest. Pendente — Capturar Ted Lasso antes/depois no mesmo perfil e resolução. Verificar retratos, textos de fallback, foco rápido, cache frio/quente e tempo de troca.
- **UI-04 · PASS_TESTE_FOCADO.** Enquadramento proporcional e degradê ancorado na borda real. Vitrine conserva 270 dp e ações. Sinopse medida em até duas linhas e selos de fundo escuro. Testes — CinematicPresentationTest; CollectionFolderCardMediaTest; CollectionLogoCropTest. Pendente — Capturar 20th Century Fox/Deadpool & Wolverine em arte clara e escura. Comparar rostos, junção à esquerda e legibilidade dos botões.
- **UI-11 · PASS_TESTE_FOCADO.** Painel de Descobrir e Biblioteca com 7 dp entre grupos, gêneros separados, nota e classificação com tratamento próprio. Recorte por medida de texto no dock, na vitrine e na grade lateral. Testes — MediaMetadataChipsTest; ScoutSynopsisTest; TraktBrowseIdentityTest. Pendente — Comparar fundos claros/escuros, títulos longos, escala de fonte e espaço útil da grade em Descobrir e Biblioteca.
- **RS-23 · PASS_TESTE_FOCADO.** Paleta escura adaptada ao tema e ao estilo, aplicada em Descubra-me, Mixes, calendário, painéis e menus afetados. Transparência relativa de 0 a 100 com piso de contraste. Cabeçalho preservado. Testes — CinematicPresentationTest; GlassMaterialTest; ClearGlassThemeTest; SignatureGlassTest; CalendarViewModelTest; ScoutReadyMixesTest. Pendente — Capturar Original, Liquid Dark e Cinematic Glass. Verificar 0/60/100%, tintas, cabeçalho, contraste e fluidez em Shield e Fire TV.
- **RS-24 · PASS_TESTE_FOCADO.** Prévia curta estática. Ler mais aparece quando há excedente e abre sob demanda o leitor rolável existente. Testes — ScoutSynopsisTest. Pendente — Testar DPAD no leitor, abertura somente por seleção, Back e restauração do foco, sinopses curtas/longas e ausência de rolagem fora da tela.

## Continuidade

Aguardar revisão do coordenador e a janela autorizada para comparação do candidato em TV. A matriz mantém a situação de todos os IDs dos megalotes posteriores.
