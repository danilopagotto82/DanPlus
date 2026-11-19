# DanPlus — migração GitHub sem histórico legado — 19/11/2026

## Escopo
- Destino oficial: https://github.com/danilopagotto82/DanPlus
- Origem de snapshot: branch danplus/mlc1-202-release-only-20261119, commit c92187031a204a4a01530f0b8662205c598b1e81, no repositório legado.
- O snapshot preserva todos os arquivos rastreados dessa fonte, exceto mudanças de segurança documentadas abaixo; o histórico Git antigo NÃO foi importado.
- Não fazer force push, não alterar tags/releases existentes, não mudar package Android, assinatura ou canal OTA.

## Ajustes aplicados somente na fonte deste novo repositório
1. Chave de fallback YouTube: substituída a constante fixa por BuildConfig.YOUTUBE_INNERTUBE_FALLBACK_KEY, alimentada por YOUTUBE_INNERTUBE_FALLBACK_KEY em local.properties ou local.dev.properties (ignorados pelo Git). Default vazio. A leitura dinâmica continua implementada. Para comportamento de fallback equivalente em build futuro é preciso configurar a propriedade local; não copiar chave privada/publicamente no Git.
2. Sete workflows do legado: movidos para docs/danplus/workflows-legado-desativados, como arquivos de referência .yml.txt. Nenhuma ação automática de build, release ou cron ativa neste repositório após a importação.
3. README: atualizado para identidade oficial e orientação de OTA preservado.
4. Fixture subtitle-redirect-test.p12: mantida como arquivo de teste (chave autoassinada/loopback de servidor de testes), sem relação com certificado de assinatura APK. O arquivo já existia no código-fonte original e não entra nos APKs distribuídos.
5. Arquivos de licença e atribuições: mantidos.

## Limites de verificação
- Testes existentes da versão-ponte: 39 JUnit PASS anteriores e build release PASS do código homologado. Homologação física: PASS manual informado pelo proprietário na Fire TV e Shield, ambas 2.0.2/1644.
- Novo snapshot sanitizado: verificação estática e varredura de credenciais; build Gradle e teste de fallback: NÃO TESTADOS. Não confundir com os APKs 2.0.2 publicados.
- Compatibilidade feed OTA na release legada 2.0.2: PASS de GitHub API e SHA256. Cliente real ainda na 2.0.0: NÃO TESTADO.
- Dois alertas de Google API Key do legado: OPEN, validade desconhecida. O segundo está em bibliotecas de histórico antigo e não foi importado. Titularidade/restrições/rotação ainda precisam de evidência de Google Cloud; não dispensar ou ignorar o alerta.

## Origem de distribuição preservada
- Versão 2.0.2: https://github.com/danilopagotto82/DANPLUS-Shield-Arc31-Private/releases/tag/2.0.2
- Shield SHA256: 964152DA6DACA8088C7DC67620F2D69AC1CCCD38CE5CF7DEBE2702CAD779328A
- Fire SHA256: A34AA70C321AB9B5F544CDAB7E74E8BB0442AB93800A2D84CB496A9FA1B2B4B0

## Antes de novos releases
- Configurar e testar a chave de fallback via propriedade local, verificar credenciais e RLS, reativar CI somente após revisão, executar testes/build/smoke.
- Preservar as bibliotecas licenciadas e manter o repositório legado ativo para atualizações anteriores.
## Dependencia Git externa do historico
- A fonte original tem gitlink libmpv-android fixado no SHA f48aa82b2677428f756a689a44c2477c3f37be51, sem .gitmodules/URL de origem verificada. O gitlink nao foi importado no snapshot para evitar referencia inoperante. O projeto possui bibliotecas AAR precompiladas em app/libs; antes de recompilar, validar dependencias e licencas desse modulo.

## Documentacao futura incorporada
- GEMINI.md e 18 documentos de planejamento do commit local a6f9db959f28791241f13117d52a07f6d3e7572b foram copiados para o repositorio oficial. Nao alteram o aplicativo 2.0.2 homologado e nao constituem autorizacao para programar.
