# DanPlus

**Android TV para NVIDIA Shield Pro e Fire TV Stick 4K Max.**

Este é o repositório oficial de código do projeto DanPlus. A versão atual é a **2.0.2 (1644)**.

## Instalar ou atualizar

[Baixar DanPlus 2.0.2 para Shield e Fire TV](https://github.com/danilopagotto82/DANPLUS-Shield-Arc31-Private/releases/tag/2.0.2)

Utilize o APK específico para seu dispositivo. **Não desinstale o aplicativo para atualizar**: a atualização normal preserva os perfis e as configurações. Preserve as assinaturas e os IDs dos pacotes Android.

## Código-fonte

O código de desenvolvimento foi importado para este repositório a partir do commit c92187031a204a4a01530f0b8662205c598b1e81, referência exata da versão-ponte 2.0.2.

**Importante:** este é um snapshot de código com ajustes de segurança para publicação, **não um rebuild do APK homologado**. O valor fixo de fallback do YouTube foi retirado da fonte pública e substituído por uma configuração local opcional. Na ausência dessa configuração, o fallback específico pode não funcionar quando o YouTube bloquear a página de configuração, embora a leitura dinâmica da chave continue disponível.

Os sete workflows herdados estão guardados como documentos, desativados até revisão. Nenhuma compilação do snapshot sanitizado foi homologada nesta migração.

[Referência exata do código usado nos APKs 2.0.2](https://github.com/danilopagotto82/DANPLUS-Shield-Arc31-Private/tree/danplus/mlc1-202-release-only-20261119)

[Relatório técnico desta migração](docs/danplus/MIGRACAO_GITHUB_FONTE_SEGURA_20261119.md)

## Compatibilidade OTA

O [repositório legado](https://github.com/danilopagotto82/DANPLUS-Shield-Arc31-Private) permanece público e ativo como origem de releases para instalações antigas. Não renomear, arquivar ou excluir esse canal sem testar atualização nos clientes anteriores.

## Segurança e contribuições

Os alertas históricos de Secret Scanning do legado continuam pendentes de classificação de titularidade, uso e restrições; não foram resolvidos nem importados como histórico no novo repositório.

Nunca inclua credenciais, arquivos de assinatura ou local.properties em commits. Para builds futuros, configure as variáveis locais necessárias e realize testes de regressão antes de publicar APKs.

## Créditos e licença

Este fork respeita as licenças e atribuições das bases Nuvio Original e Nuvio ARC, além das dependências de terceiros. Consulte [LICENSE](LICENSE), os arquivos de atribuição e as licenças incluídas no código.
## Planejamento de melhorias
O **[Plano Único DanPlus 2.1.0](docs/danplus/PLANO_DANPLUS_2_1_0.md)** é a fonte oficial de planejamento para todas as ferramentas e IAs. Reúne megalotes, decisões, requisitos de UI (incluindo **Signature Glass**) e gates de segurança. Os 17 arquivos em `docs/danplus/gemini-planejamento/` são **anexos técnicos históricos**, preservados para consulta, não outro planejamento. Nada disso é funcionalidade implementada na 2.0.2.
