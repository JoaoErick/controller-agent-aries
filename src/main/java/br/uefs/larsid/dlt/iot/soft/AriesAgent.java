package br.uefs.larsid.dlt.iot.soft;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import br.uefs.larsid.dlt.iot.soft.mqtt.Listener;
import br.uefs.larsid.dlt.iot.soft.mqtt.MQTTClient;
import br.uefs.larsid.dlt.iot.soft.utils.CLI;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Scanner;

import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.connection.ConnectionAcceptInvitationFilter;
import org.hyperledger.aries.api.connection.ConnectionAcceptRequestFilter;
import org.hyperledger.aries.api.connection.ConnectionReceiveInvitationFilter;
import org.hyperledger.aries.api.connection.ConnectionRecord;
import org.hyperledger.aries.api.connection.CreateInvitationRequest;
import org.hyperledger.aries.api.connection.CreateInvitationResponse;
import org.hyperledger.aries.api.connection.ReceiveInvitationRequest;
import org.hyperledger.aries.api.credential_definition.CredentialDefinition;
import org.hyperledger.aries.api.credential_definition.CredentialDefinition.CredentialDefinitionRequest;
import org.hyperledger.aries.api.credential_definition.CredentialDefinition.CredentialDefinitionResponse;
import org.hyperledger.aries.api.credential_definition.CredentialDefinitionFilter;
import org.hyperledger.aries.api.credentials.Credential;
import org.hyperledger.aries.api.credentials.CredentialAttributes;
import org.hyperledger.aries.api.credentials.CredentialPreview;
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeRole;
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeState;
import org.hyperledger.aries.api.issue_credential_v1.IssueCredentialRecordsFilter;
import org.hyperledger.aries.api.issue_credential_v1.V1CredentialExchange;
import org.hyperledger.aries.api.issue_credential_v1.V1CredentialProposalRequest;
import org.hyperledger.aries.api.issue_credential_v1.V1CredentialStoreRequest;
import org.hyperledger.aries.api.present_proof.PresentProofRecordsFilter;
import org.hyperledger.aries.api.present_proof.PresentProofRequest;
import org.hyperledger.aries.api.present_proof.PresentationExchangeRecord;
import org.hyperledger.aries.api.present_proof.PresentationExchangeRole;
import org.hyperledger.aries.api.present_proof.PresentationExchangeState;
import org.hyperledger.aries.api.revocation.RevokeRequest;
import org.hyperledger.aries.api.schema.SchemaSendRequest;
import org.hyperledger.aries.api.schema.SchemaSendResponse;
import org.hyperledger.aries.api.schema.SchemaSendResponse.Schema;
import org.hyperledger.aries.api.schema.SchemasCreatedFilter;

/**
 *
 * @author Emers
 */
public class AriesAgent {

    private static final String SCHEMA_ID = "WRSprCn3VTQU5ssmQJQajH:2:soft-iot-gateway:1.0";
    private static final boolean DEBUG_MODE = true;

    /* -------------------------- Topic constants ---------------------------- */
    private static final String CREATE_INVITATION = "POST CREATE_INVITATION";
    private static final String ACCEPT_INVITATION = "POST ACCEPT_INVITATION";
    private static final String CREDENTIAL_DEFINITIONS = "POST CREDENTIAL_DEFINITIONS";
    private static final String ISSUE_CREDENTIAL = "POST ISSUE_CREDENTIAL";
    /* ---------------------------------------------------------------------- */

    /* -------------------------- MQTT constants ---------------------------- */
    private static final int MQTT_QOS = 1;
    /* ---------------------------------------------------------------------- */

    /* ---------------------- Controller properties ------------------------- */
    private static String endpoint;
    private static String credentialDefinitionId;
    private static String brokerIp;
    private static String brokerPort;
    private static String brokerUsername;
    private static String brokerPassword;
    private static String agentIp;
    private static String agentPort;
    /* ---------------------------------------------------------------------- */

    private static AriesClient ac;
    private static String did;
    private static String presentationId;
    private static MQTTClient mqttClient;

    public static void main(String[] args) throws IOException, InterruptedException {
        readProperties(args);

        mqttClient = new MQTTClient(DEBUG_MODE, brokerIp, brokerPort, brokerUsername, brokerPassword);

        mqttClient.connect();

        String[] topics = {
                CREATE_INVITATION,
                ACCEPT_INVITATION,
                CREDENTIAL_DEFINITIONS,
                ISSUE_CREDENTIAL
        };

        new Listener(
                mqttClient,
                topics,
                MQTT_QOS,
                DEBUG_MODE);

        AriesClient ac = getAriesClient(agentIp, agentPort);

        did = ac.walletDidPublic().get().getDid(); // Did publico
        presentationId = null;

        printlnDebug(">> Controller Aries Agent is running...");
    }

    public static void readProperties(String[] args) {
        try (InputStream input = AriesAgent.class.getResourceAsStream("controller.properties")) {
            if (input == null) {
                printlnDebug("Sorry, unable to find controller.properties.");
                return;
            }
            Properties props = new Properties();
            props.load(input);

            endpoint = CLI.getEndpoint(args)
                    .orElse(props.getProperty("endpoint"));

            credentialDefinitionId = CLI.getCredentialDefinitionId(args)
                    .orElse(props.getProperty("credentialDefinitionId"));

            brokerIp = CLI.getBrokerIp(args)
                    .orElse(props.getProperty("brokerIp"));

            brokerPort = CLI.getBrokerPort(args)
                    .orElse(props.getProperty("brokerPort"));

            brokerPassword = CLI.getBrokerPassword(args)
                    .orElse(props.getProperty("brokerPassword"));

            brokerUsername = CLI.getBrokerUsername(args)
                    .orElse(props.getProperty("brokerUsername"));

            agentIp = CLI.getAgentIp(args)
                    .orElse(props.getProperty("agentIp"));

            agentPort = CLI.getAgentPort(args)
                    .orElse(props.getProperty("agentPort"));

        } catch (IOException ex) {
            printlnDebug("Sorry, unable to find sensors.json or not create pesistence file.");
        }
    }

    // Obtem uma instância do Aries Cloud Agent
    public static AriesClient getAriesClient(String AGENT_ADDR, String AGENT_PORT) throws IOException {
        if (ac == null) {
            ac = AriesClient.builder().url("http://" + AGENT_ADDR + ":" + AGENT_PORT).build(); // Public network
                                                                                               // BuilderNetwork
            // ac = AriesClient.builder().url("http://localhost:8001").build(); //Local
            // network VonNetwork
        }
        System.out.println(">> Agente Inicializado: " + ac.statusLive().get().isAlive());
        System.out.println(">> Public DID Information: " + ac.walletDidPublic().get().getDid());
        return ac;
    }

    // Gera um invitation url para conexão
    public static String createInvitation(String nodeUri) throws IOException {
        return createInvitation(ac, endpoint, nodeUri);
    }

    // Gera um invitation url para conexão
    private static String createInvitation(AriesClient ac, String END_POINT, String nodeUri) throws IOException {
        Optional<CreateInvitationResponse> responseCI = ac.connectionsCreateInvitation(
                CreateInvitationRequest.builder().myLabel("Agent_Three").serviceEndpoint(END_POINT).build());
        printlnDebug(">> Invitation URL: " + responseCI.get().getInvitationUrl());

        return "{" +
                "\"invitationURL\":\"" + responseCI.get().getInvitationUrl() + "\"," +
                "\"nodeUri\":\"" + nodeUri + "\"," +
                "\"connectionId\":\"" + responseCI.get().getConnectionId() + "\""
                +
                "}";
    }

    // Gera o schema de forma estática
    public static Optional<SchemaSendResponse> createSchema(AriesClient ac) throws IOException {
        String schemaName = "soft-iot-gateway";

        String version = "1.0";

        List<String> attributes = attributesSchema();

        Optional<SchemaSendResponse> response = ac.schemas(
                SchemaSendRequest.builder()
                        .schemaName(schemaName)
                        .schemaVersion(version)
                        .attributes(attributes)
                        .build());

        printlnDebug("Schema:");
        printlnDebug(response.get().toString());

        return response;
    }

    // // Gera o schema de forma estática
    // public static Optional<SchemaSendResponse> createSchema(AriesClient ac,
    // Scanner scan) throws IOException {
    // System.out.println("Informe o nome do Schema: ");
    // String schemaName = scan.next();

    // System.out.println("Informe a versão do Schema: ");
    // String version = scan.next();

    // List<String> attributes = attributesSchema(scan);

    // Optional<SchemaSendResponse> response = ac.schemas(
    // SchemaSendRequest.builder()
    // .schemaName(schemaName)
    // .schemaVersion(version)
    // .attributes(attributes)
    // .build());

    // System.out.println("Schema:");
    // System.out.println(response.get().toString());

    // return response;
    // }

    // Método para adicionar os atributos do schema
    public static List<String> attributesSchema() {
        List<String> attributes = new LinkedList<String>();
        attributes.add("id");

        return attributes;
    }

    // // Método para obter os atributos para o schema
    // public static List<String> attributesSchema(Scanner scan) {
    // List<String> attributes = new LinkedList<String>();

    // while (true) {
    // System.out.println("Informe o Nome do atributo ou digite 0 para encerrar: ");
    // String atr = scan.next();

    // if (atr.equals("0")) {
    // break;
    // } else {
    // attributes.add(atr);
    // }
    // }

    // return attributes;
    // }

    /* Cria uma definição de credencial */
    public static String credentialDefinition() throws IOException {
        return credentialDefinition(ac);
    }

    // Envia uma definição de credencial para o ledger (blockchain), nesse caso
    // utiliza a schema estático criado no método acima ( createSchema() )
    public static String credentialDefinition(AriesClient ac) throws IOException {
        Optional<CredentialDefinition> credentialDefinition =
        ac.credentialDefinitionsGetById(credentialDefinitionId);

        if (credentialDefinition.isEmpty()) {
            printlnDebug(">> Empty ");
            Optional<SchemaSendResponse> schema = createSchema(ac);

            Optional<CredentialDefinitionResponse> response = ac
                    .credentialDefinitionsCreate(
                            CredentialDefinitionRequest.builder().schemaId(schema.get().getSchemaId())
                                    .supportRevocation(false).tag("Agent_Three").revocationRegistrySize(1000).build());

            printlnDebug(">> Credential Definition created: ");
            printlnDebug(response.get().toString());

            credentialDefinitionId = response.get().getCredentialDefinitionId();

            try (InputStream input = AriesAgent.class.getResourceAsStream("controller.properties")) {
                if (input == null) {
                    printlnDebug("Sorry, unable to find controller.properties.");
                }
                Properties props = new Properties();
                props.load(input);

                props.setProperty("credentialDefinitionId", credentialDefinitionId);

            } catch (IOException e) {
                printlnDebug("(!) Error changing a property");
            }
        } else {
            printlnDebug(">> Credential Definition already exists!");
        }

        return credentialDefinitionId;
    }

    // Lista todos os schemas criados
    public static String getSchema(AriesClient ac) throws IOException {
        Optional<SchemaSendResponse.Schema> response = ac.schemasGetById(SCHEMA_ID);
        if (!response.isEmpty()) {
            return response.get().getId();
        }
        return null;
    }

    // Lista todos os schemas criados
    public static List<String> getSchemas(AriesClient ac) throws IOException {
        Optional<List<String>> schemas = ac.schemasCreated(SchemasCreatedFilter.builder().build());
        return schemas.get();
    }

    // Lista todos os schemas criados
    public static void showSchemas(AriesClient ac) throws IOException {
        Optional<List<String>> schemas = ac.schemasCreated(SchemasCreatedFilter.builder().build());
        System.out.println(schemas.get().toString());
    }

    // Lista um schema através do id informado (lista mais informações do id gerado)
    public static void getSchemaById(AriesClient ac, Scanner scan) throws IOException {
        System.out.print("Informe o SchemaId: ");
        Optional<Schema> schema = ac.schemasGetById(scan.next());

        System.out.println("\nId Schema: " + schema.get().getId());
        System.out.println("Name: " + schema.get().getName());
        System.out.println("Versão: " + schema.get().getVersion());
        System.out.println("Atributos: " + schema.get().getAttrNames().toString());
        System.out.println("seqNo: " + schema.get().getSeqNo() + "\n");
    }

    // Obtem o id de uma definição de credencial
    public static List<String> getCredentialDefinition(String did) throws IOException {

        CredentialDefinitionFilter filter = CredentialDefinitionFilter.builder().issuerDid(did).build();

        Optional<CredentialDefinition.CredentialDefinitionsCreated> response = ac.credentialDefinitionsCreated(filter);

        int i = 0;
        for (String credentialDefinitionId : response.get().getCredentialDefinitionIds()) {
            System.out.println("Number: " + i++);
            System.out.println(credentialDefinitionId);
        }
        System.out.println();

        return response.get().getCredentialDefinitionIds();
    }

    // Lista todas as conexões realizadas
    public static List<ConnectionRecord> getConnections(AriesClient ac) throws IOException {
        Optional<List<ConnectionRecord>> records = ac.connections();

        for (int i = 0; i < records.get().size(); i++) {
            System.out.println("Number: " + i + "\n");
            System.out.println("State: " + records.get().get(i).getState());
            System.out.println("RFC23 State: " + records.get().get(i).getRfc23Sate());
            System.out.println("Created at: " + records.get().get(i).getCreatedAt());
            System.out.println("Connection Id: " + records.get().get(i).getConnectionId());
            System.out.println("Their Label: " + records.get().get(i).getTheirLabel());
            System.out.println("Their DID: " + records.get().get(i).getTheirDid());
            System.out.println("Their Role: " + records.get().get(i).getTheirRole());
            System.out.println("Invitation Key: " + records.get().get(i).getInvitationKey() + "\n");
        }
        return records.get();
    }

    // Envia uma credencial no modelo v1.0
    public static void issueCredentialV1(JsonObject jsonProperties) throws IOException {
        issueCredentialV1(ac, jsonProperties, did);
    }

    // Envia uma credencial no modelo v1.0
    private static void issueCredentialV1(AriesClient ac, JsonObject jsonProperties, String did) throws IOException {
        String connectionId = jsonProperties.get("connectionId").getAsString();
        String value = jsonProperties.get("value").getAsString();

        if (credentialDefinitionId.isEmpty()) {
            printlnDebug(">> Credential Definition is not registered!");
        } else {
            List<CredentialAttributes> attributes = new LinkedList<CredentialAttributes>();
            attributes.add(new CredentialAttributes("id", value));

            CredentialPreview credPrev = new CredentialPreview(attributes);

            Optional<V1CredentialExchange> response = ac.issueCredentialSend(
                    V1CredentialProposalRequest.builder()
                            .connectionId(connectionId)
                            .credentialDefinitionId(credentialDefinitionId)
                            .autoRemove(true)
                            .credentialProposal(credPrev)
                            .build());

            printlnDebug(response.get().toString());
        }

    }

    // public static void issueCredentialV1(AriesClient ac, Scanner scan, String
    // did) throws IOException {
    // List<ConnectionRecord> connections = getConnections(ac);
    // System.out.println("Informe o Numero da Conexão: ");
    // int conNum = scan.nextInt();
    // String conId = connections.get(conNum).getConnectionId();

    // List<String> credentialsDefinitionsIds = getCredentialDefinition(did);

    // System.out.println("\nInforme o number da Definition: ");
    // int credDefNumber = scan.nextInt();
    // String credDefId = credentialsDefinitionsIds.get(credDefNumber);

    // List<CredentialAttributes> attributes = new
    // LinkedList<CredentialAttributes>();
    // attributes.add(new CredentialAttributes("nome", "Aluno 3"));
    // attributes.add(new CredentialAttributes("email", "aluno@ecomp.uefs.br"));
    // attributes.add(new CredentialAttributes("matricula", "12345678"));

    // CredentialPreview credPrev = new CredentialPreview(attributes);

    // Optional<V1CredentialExchange> response = ac.issueCredentialSend(
    // V1CredentialProposalRequest.builder()
    // .connectionId(conId)
    // .credentialDefinitionId(credDefId)
    // .autoRemove(true)
    // .credentialProposal(credPrev)
    // .build());

    // System.out.println("\n" + response.get().toString());
    // }

    // Solicita uma apresentação de credencial
    public static String requestProofCredential(AriesClient ac, String did) throws IOException, InterruptedException {
        String connectionId = getConnections(ac).get(0).getConnectionId();
        String comment = "Prove que é aluno";
        String nameOfProofRequest = "Prova de educação";
        String nameOfAttrRequest = "nome";
        String version = "1.0";
        String valueOfAttrRequest = "nome";
        String restrictionName = "cred_def_id";
        String restrictionValue = getCredentialDefinition(did).get(0);

        JsonObject restriction = new JsonObject();
        restriction.addProperty(restrictionName, restrictionValue);
        PresentProofRequest.ProofRequest.ProofRequestedAttributes requestedAttributeValue = PresentProofRequest.ProofRequest.ProofRequestedAttributes
                .builder().restriction(restriction).name(valueOfAttrRequest).build();
        PresentProofRequest.ProofRequest proofRequest = PresentProofRequest.ProofRequest.builder()
                .requestedAttribute(nameOfAttrRequest, requestedAttributeValue).name(nameOfProofRequest)
                .version(version).build();

        PresentProofRequest presentProofRequest = PresentProofRequest.builder().comment(comment)
                .connectionId(connectionId).proofRequest(proofRequest).build();

        System.out.println(presentProofRequest);
        System.out.println(presentProofRequest.getProofRequest());
        Optional<PresentationExchangeRecord> presentationExchangeRecord = ac
                .presentProofSendRequest(presentProofRequest);

        Optional<PresentationExchangeRecord> pres;
        String presentationId = presentationExchangeRecord.get().getPresentationExchangeId();
        PresentationExchangeRecord presentation;

        do {
            pres = ac.presentProofRecordsGetById(presentationId);
            presentation = pres.get();
            System.out.println("UpdateAt: " + presentation.getUpdatedAt());
            System.out.println("Presentation: " + presentation.getPresentation());
            System.out.println("Verificada: " + presentation.isVerified());
            System.out.println("State: " + presentation.getState());
            System.out.println("Auto Presentation: " + presentation.getAutoPresent());
            Thread.sleep(2 * 1000);
        } while (!presentation.getState().equals(PresentationExchangeState.PRESENTATION_RECEIVED));

        return presentationId;
    }

    // verificação da apresentação
    public static boolean verifyPresentation(AriesClient ac, String presentationId) throws IOException {
        if (presentationId != null) {
            boolean response = ac.presentProofRecordsVerifyPresentation(presentationId).get().getVerified();
            System.out.println("Apresentada: " + response);
            return response;
        }
        return false;
    }

    // recepção de convite
    public static void receiveInvitation(AriesClient ac) throws IOException {
        String type = "";
        String id = "";
        String did = "";
        List<String> recipientKeys = new ArrayList<>();
        String label = "";
        String serviceUrl = "";
        List<String> routingKeys = new ArrayList<>();
        String imageUrl = "";

        ReceiveInvitationRequest invite = ReceiveInvitationRequest.builder().type(type).id(id).did(did)
                .recipientKeys(recipientKeys).label(label).serviceEndpoint(serviceUrl).routingKeys(routingKeys)
                .imageUrl(imageUrl).build();

        String alias = "";
        Boolean autoAccept = true;
        String mediationId = "";

        ConnectionReceiveInvitationFilter filter = ConnectionReceiveInvitationFilter.builder().alias(alias)
                .autoAccept(autoAccept).mediationId(mediationId).build();

        Optional<ConnectionRecord> connectionRecord = ac.connectionsReceiveInvitation(invite, filter);

        ConnectionRecord connectionRecord1 = connectionRecord.get();
    }

    // aceitação de convite
    public static void acceptInvitation(AriesClient ac) throws IOException {
        String connectionId = "";
        String mediationId = "";
        String myEndPoint = "";
        String myLabel = "";

        ConnectionAcceptInvitationFilter filter = ConnectionAcceptInvitationFilter.builder().mediationId(mediationId)
                .myEndpoint(myEndPoint).myLabel(myLabel).build();

        Optional<ConnectionRecord> connectionRecord = ac.connectionsAcceptInvitation(connectionId, filter);

        ConnectionRecord connectionRecord1 = connectionRecord.get();

    }

    // confirmando a aceitação
    public static void acceptRequest(AriesClient ac) throws IOException {
        String connectionId = "";
        String myEndPoint = "";

        ConnectionAcceptRequestFilter filter = ConnectionAcceptRequestFilter.builder().myEndpoint(myEndPoint).build();

        Optional<ConnectionRecord> connectionRecord = ac.connectionsAcceptRequest(connectionId, filter);

        ConnectionRecord connectionRecord1 = connectionRecord.get();

    }

    // recuperando credenciais gravadas
    public static void getCredentialExchange(AriesClient ac) throws IOException {
        String connectionId = "";
        CredentialExchangeRole credentialExchangeRole = CredentialExchangeRole.HOLDER;
        CredentialExchangeState credentialExchangeState = CredentialExchangeState.CREDENTIAL_RECEIVED;
        String threadId = "";

        IssueCredentialRecordsFilter filter = IssueCredentialRecordsFilter.builder().connectionId(connectionId)
                .role(credentialExchangeRole).state(credentialExchangeState).threadId(connectionId).build();

        Optional<List<V1CredentialExchange>> credentialsExchange = ac.issueCredentialRecords(filter);

        List<V1CredentialExchange> credentialsExchange1 = credentialsExchange.get();
    }

    // armazenar credencial na carteira
    public static void storeCredentialWallet(AriesClient ac) throws IOException {
        String credentialExchangeId = "";
        String credentialId = "";

        V1CredentialStoreRequest request = V1CredentialStoreRequest.builder().credentialId(credentialId).build();

        Optional<V1CredentialExchange> credentialExchange = ac.issueCredentialRecordsStore(credentialExchangeId,
                request);

        V1CredentialExchange credentialExchange1 = credentialExchange.get();
    }

    // verificar recepção de prova
    public static void presentProof(AriesClient ac) throws IOException {
        String connectionId = "";
        PresentationExchangeRole presentationExchangeRole = PresentationExchangeRole.PROVER;
        PresentationExchangeState presentationExchangeState = PresentationExchangeState.PRESENTATION_RECEIVED;
        String threadId = "";

        PresentProofRecordsFilter filter = PresentProofRecordsFilter.builder().connectionId(connectionId)
                .role(presentationExchangeRole).state(presentationExchangeState).threadId(threadId).build();

        Optional<List<PresentationExchangeRecord>> presentationExchangeRecords = ac.presentProofRecords(filter);

        List<PresentationExchangeRecord> presentationExchangeRecords1 = presentationExchangeRecords.get();

    }

    // Aceita conexão com outro agente
    public static void receiveInvitation(JsonObject jsonProperties) throws IOException {
        receiveInvitation(ac, jsonProperties);
    }

    // Aceita conexão com outro agente
    private static void receiveInvitation(AriesClient ac, JsonObject jsonProperties) throws IOException {
        String type, id, label, serviceEndpoint;
        JsonArray recipientKeysJsonArray;

        type = jsonProperties.get("@type").getAsString();
        id = jsonProperties.get("@id").getAsString();
        label = jsonProperties.get("label").getAsString();
        serviceEndpoint = jsonProperties.get("serviceEndpoint").getAsString();
        recipientKeysJsonArray = jsonProperties.get("recipientKeys").getAsJsonArray();

        List<String> recipientKeys = new Gson().fromJson(recipientKeysJsonArray, LinkedList.class);

        ReceiveInvitationRequest request = new ReceiveInvitationRequest().builder()
                .id(id)
                .type(type)
                .label(label)
                .serviceEndpoint(serviceEndpoint)
                .recipientKeys(recipientKeys)
                .build();

        ConnectionReceiveInvitationFilter filter = ConnectionReceiveInvitationFilter.builder()
                .autoAccept(true).build();

        Optional<ConnectionRecord> record = ac.connectionsReceiveInvitation(request, null);

        printlnDebug(record.get().toString());
    }

    // Lista as credenciais recebidas
    public static void getCredentialsReceive(AriesClient ac) throws IOException {
        Optional<List<Credential>> credentials = ac.credentials();
        System.out.println("\n");

        for (int i = 0; i < credentials.get().size(); i++) {
            System.out.println("Schema ID da Credencial: " + credentials.get().get(i).getSchemaId());
            System.out.println(
                    "Id Credential Definition da Credencial: " + credentials.get().get(i).getCredentialDefinitionId());
            System.out.println("referent da Credencial: " + credentials.get().get(i).getReferent());
            System.out.println("rev_reg_id: " + credentials.get().get(i).getRevRegId());
            System.out.println("cred_rev_id: " + credentials.get().get(i).getCredRevId());
            System.out.println("Atributos: " + credentials.get().get(i).getAttrs().toString());

            System.out.println("\n");
        }
    }

    public static void revokeCredential(AriesClient ac) throws IOException {
        Scanner scan = new Scanner(System.in);

        System.out.println("Credential Exchange ID: ");
        String credExId = scan.next();
        System.out.println("Revocation Register ID: ");
        String revRegId = scan.next();
        System.out.println("Credetial revocation ID: ");
        String credRevId = scan.next();

        System.out.println("credExId: " + credExId);
        System.out.println("revRegId: " + revRegId);
        System.out.println("credRevId: " + revRegId);

        RevokeRequest revokeRequest = RevokeRequest.builder().revRegId(revRegId).credRevId(credRevId).publish(true)
                .build(); // Pode ser usado esse
        // RevokeRequest revokeRequest =
        // RevokeRequest.builder().credExId(credExId).publish().build(); //Ou esse

        ac.revocationRevoke(revokeRequest);
    }

    private static void printlnDebug(String str) {
        if (isDebugModeValue()) {
            System.out.println(str);
        }
    }

    public static boolean isDebugModeValue() {
        return DEBUG_MODE;
    }
}
