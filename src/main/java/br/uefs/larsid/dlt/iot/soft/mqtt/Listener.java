package br.uefs.larsid.dlt.iot.soft.mqtt;

import java.util.Base64;

import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import br.uefs.larsid.dlt.iot.soft.AriesAgent;

public class Listener implements IMqttMessageListener {

  private static final int QOS = 1;

  /* -------------------------- Topic constants ---------------------------- */
  private static final String CREATE_INVITATION = "POST CREATE_INVITATION";
  private static final String ACCEPT_INVITATION = "POST ACCEPT_INVITATION";
  private static final String CREDENTIAL_DEFINITIONS = "POST CREDENTIAL_DEFINITIONS";
  private static final String ISSUE_CREDENTIAL = "POST ISSUE_CREDENTIAL";
  private static final String REQUEST_PROOF_CREDENTIAL = "POST REQUEST_PROOF_CREDENTIAL";
  /* ---------------------------------------------------------------------- */

  /*
   * -------------------------- Topic Res constants ----------------------------
   */
  private static final String CREATE_INVITATION_RES = "CREATE_INVITATION_RES";
  private static final String CREDENTIAL_DEFINITIONS_RES = "CREDENTIAL_DEFINITIONS_RES";
  private static final String ACCEPT_INVITATION_RES = "ACCEPT_INVITATION_RES";
  private static final String ISSUE_CREDENTIAL_RES = "ISSUE_CREDENTIAL_RES";
  private static final String REQUEST_PROOF_CREDENTIAL_RES = "REQUEST_PROOF_CREDENTIAL_RES";
  /* ---------------------------------------------------------------------- */

  private boolean debugModeValue;
  private MQTTClient mqttClient;

  /**
   * Método Construtor.
   *
   * @param mqttClient     MQTTClient - Cliente MQTT.
   * @param topics         String[] - Tópicos que serão assinados.
   * @param qos            int - Qualidade de serviço do tópico que será ouvido.
   * @param debugModeValue boolean - Modo para debugar o código.
   */
  public Listener(
      MQTTClient mqttClient,
      String[] topics,
      int qos,
      boolean debugModeValue) {
    this.mqttClient = mqttClient;
    this.debugModeValue = debugModeValue;

    for (String topic : topics) {
      this.mqttClient.subscribe(qos, this, topic);
    }
  }

  @Override
  public void messageArrived(String topic, MqttMessage message)
      throws Exception {
    String msg = new String(message.getPayload());

    printlnDebug("==== Receive Request ====");

    /* Verificar qual o tópico recebido. */
    switch (topic) {
      case CREATE_INVITATION:
        String result = AriesAgent.createInvitation(msg);
        printlnDebug(">> Send Invitation URL...");

        JsonObject jsonResult = new Gson().fromJson(result, JsonObject.class);

        byte[] payload = jsonResult.toString().getBytes();
        this.mqttClient.publish(CREATE_INVITATION_RES, payload, QOS);
        break;
      case ACCEPT_INVITATION:
        // Removendo Ip e Porta da URL
        msg = msg.substring(msg.indexOf("=") + 1);
        printlnDebug(msg);

        byte[] decodedBytes = Base64.getDecoder().decode(msg);
        String decodedString = new String(decodedBytes);
        printlnDebug(decodedString);

        JsonObject jsonInvitation = new Gson().fromJson(decodedString, JsonObject.class);
        AriesAgent.receiveInvitation(jsonInvitation);

        printlnDebug(">> Invitation Accepted!");
        this.mqttClient.publish(ACCEPT_INVITATION_RES, "".getBytes(), QOS);

        break;
      case CREDENTIAL_DEFINITIONS:
        printlnDebug("CREDENTIAL_DEFINITIONS");
        AriesAgent.credentialDefinition();

        this.mqttClient.publish(CREDENTIAL_DEFINITIONS_RES, "".getBytes(), QOS);

        break;
      case ISSUE_CREDENTIAL:
        JsonObject jsonCredential = new Gson().fromJson(msg, JsonObject.class);
        AriesAgent.issueCredentialV1(jsonCredential);

        this.mqttClient.publish(ISSUE_CREDENTIAL_RES, "".getBytes(), QOS);

        break;
      case REQUEST_PROOF_CREDENTIAL:
        JsonObject jsonRequestProof = new Gson().fromJson(msg, JsonObject.class);
        AriesAgent.requestProofCredential(jsonRequestProof);

        this.mqttClient.publish(REQUEST_PROOF_CREDENTIAL_RES, "".getBytes(), QOS);

        break;
      default:
        printlnDebug("Unrecognized topic!");
        break;
    }
  }

  private void printlnDebug(String str) {
    if (isDebugModeValue()) {
      System.out.println(str);
    }
  }

  public boolean isDebugModeValue() {
    return debugModeValue;
  }
}
