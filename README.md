# Controller Agent Aries

O `controller-agent-aries` é o projeto responsável por fazer a intermediação entre o soft-iot-base e o _Aries Agent_ (aca-py). Ele é capaz de publicar/assinar em tópicos para se comunicar com bundle [soft-iot-dlt-client-hyperledger-aries](https://github.com/JoaoErick/soft-iot-dlt-client-hyperledger-aries) e enviar requisições HTTP para o [_Aries Agent_ (aca-py)](https://github.com/hyperledger/aries-cloudagent-python).

## Modelo da arquitetura

<p align="center">
  <img src="./assets/controller-aries-comunication.png" width="550px" />
</p>

---

## Interações com o __Aries Agent__

| Funções                | Descrição                                        |
| ---------------------- | ------------------------------------------------ |
| getAriesClient()       | Obtém uma instância do Aries Cloud Agent         |
| credentialDefinition() | Criação e envio de uma definição de credencial para o ledger |
| createInvitation() | Recebe uma URL de conexão |
| receiveInvitation() | Solicita a conexão entre agentes a partir da URL recebida |
| issueCredentialV1() | Solicita o envio de uma credencial v1.0 para um agente |

---

## Interações MQTT com o bundle __Client-Hyperledger-Aries__

| Tópicos de Assinatura       | Descrição                                        |
| --------------------------- | ------------------------------------------------ |
| POST CREDENTIAL_DEFINITIONS | Recebe a solicitação para criar uma definição de credencial |
| POST CREATE_INVITATION      | Recebe a solicitação para gerar uma URL de conexão |
| POST ACCEPT_INVITATION      | Recebe a solicitação para estabelecer a conexão entre agentes |
| POST ISSUE_CREDENTIAL       | Recebe a solicitação para emitir uma credencial para um outro agente |

| Tópicos de Publicação       | Descrição                                        |
| --------------------------- | ------------------------------------------------ |
| CREDENTIAL_DEFINITIONS_RES  | Envia a confirmação de que a definição de credencial foi criada ou resgatada |
| CREATE_INVITATION_RES       | Envia a URL de conexão gerada |
| ACCEPT_INVITATION_RES       | Envia a confirmação de que a conexão entre agentes foi estabelecida |

---