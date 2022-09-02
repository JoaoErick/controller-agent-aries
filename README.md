# Controller Agent Aries

O `controller-agent-aries` é o projeto responsável por fazer a intermediação entre o soft-iot-base e o _Aries Agent_ (aca-py). Ele é capaz de publicar/assinar em tópicos para se comunicar com bundle [soft-iot-dlt-client-hyperledger-aries](https://github.com/JoaoErick/soft-iot-dlt-client-hyperledger-aries) e enviar requisições HTTP para o [_Aries Agent_ (aca-py)](https://github.com/hyperledger/aries-cloudagent-python).

## Modelo da arquitetura

<p align="center">
  <img src="./assets/controller-aries-comunication.png" width="550px" />
</p>