package ma.enset.ga.sma;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import ma.enset.ga.sequencial.GAUtils;
import ma.enset.ga.sequencial.Individual;

import java.util.*;

public class MainAgentGA extends Agent {
    List<AgentFitness> agentsFitness=new ArrayList<>();
    Random rnd=new Random();
    @Override
    protected void setup() {
        DFAgentDescription dfAgentDescription=new DFAgentDescription();
        ServiceDescription serviceDescription=new ServiceDescription();
        serviceDescription.setType("ga");
        dfAgentDescription.addServices(serviceDescription);
        try {
            DFAgentDescription[] agentsDescriptions = DFService.search(this, dfAgentDescription);

            for (DFAgentDescription dfAD:agentsDescriptions) {
                agentsFitness.add(new AgentFitness(dfAD.getName(),0));
            }
        } catch (FIPAException e) {
            e.printStackTrace();
        }
        calculateFintness();

        SequentialBehaviour sequentialBehaviour = new SequentialBehaviour();

        sequentialBehaviour.addSubBehaviour(new Behaviour() {
            int cpt=0;
            @Override
            public void action() {
                ACLMessage receivedMSG = receive();
                if (receivedMSG!=null){
                    cpt++;
                    System.out.println(cpt);
                    int fintess=Integer.parseInt(receivedMSG.getContent());
                    AID sender=receivedMSG.getSender();
                    //System.out.println(sender.getName()+" "+fintess);
                    setAgentFintess(sender,fintess);
                    if(cpt==GAUtils.POPULATION_SIZE){
                        Collections.sort(agentsFitness,Collections.reverseOrder());
                        showPopulation();
                    }
                }else {
                    block();
                }
            }

            @Override
            public boolean done() {
                return cpt == GAUtils.POPULATION_SIZE;
            }

        });
        sequentialBehaviour.addSubBehaviour(new Behaviour() {
            int it = 0;
            AgentFitness agent1;
            AgentFitness agent2;

            @Override
            public void action() {
                selection();
                crossover();
                Collections.sort(agentsFitness,Collections.reverseOrder());
                sendMessage(agentsFitness.get(0).getAid(), "chromosome", ACLMessage.REQUEST);
                ACLMessage aclMessage = blockingReceive();
                System.out.println(it + "   " + aclMessage.getContent() + " : " + agentsFitness.get(0).getFitness());
                it++;
            }

            private void selection(){
                agent1 = agentsFitness.get(0);
                agent2 = agentsFitness.get(1);

                sendMessage(agent1.getAid(), "chromosome", ACLMessage.REQUEST);
                sendMessage(agent2.getAid(), "chromosome", ACLMessage.REQUEST);

            }

            private void crossover(){
                ACLMessage aclMessage = blockingReceive();
                ACLMessage aclMessage1 = blockingReceive();

                int pointCroisment=rnd.nextInt(GAUtils.CHROMOSOME_SIZE - 2);
                pointCroisment++;
                char[] chromosoneParent1 = aclMessage.getContent().toCharArray();
                char[] chromosoneParent2 = aclMessage1.getContent().toCharArray();
                char[] chromOffsring1 = new char[GAUtils.CHROMOSOME_SIZE];
                char[] chromOffsring2 = new char[GAUtils.CHROMOSOME_SIZE];

                for (int i=0;i<chromosoneParent1.length;i++) {
                    chromOffsring1[i]=chromosoneParent1[i];
                    chromOffsring2[i]=chromosoneParent2[i];
                }
                for (int i=0;i<pointCroisment;i++) {
                    chromOffsring1[i]=chromosoneParent2[i];
                    chromOffsring2[i]=chromosoneParent1[i];
                }
                int fitness=0;
                for (int i=0;i<GAUtils.CHROMOSOME_SIZE;i++) {
                    if(chromOffsring1[i]==GAUtils.SOLUTION.charAt(i))
                        fitness+=1;
                }
                agentsFitness.get(GAUtils.POPULATION_SIZE - 2).setFitness(fitness);

                ACLMessage message =new ACLMessage(ACLMessage.REQUEST);
                message.setContent(new String(chromOffsring1));
                message.addReceiver(agentsFitness.get(GAUtils.POPULATION_SIZE - 2).getAid());
                send(message);
                ACLMessage message2 =new ACLMessage(ACLMessage.REQUEST);
                message2.setContent(new String(chromOffsring2));
                message2.addReceiver(agentsFitness.get(GAUtils.POPULATION_SIZE - 2).getAid());
                send(message2);
                ACLMessage receiveMSG1 = blockingReceive();
                ACLMessage receiveMSG2 = blockingReceive();
                setAgentFintess(receiveMSG1.getSender(),Integer.parseInt(receiveMSG1.getContent()));
                setAgentFintess(receiveMSG2.getSender(),Integer.parseInt(receiveMSG2.getContent()));

                //                System.out.println(aclMessage1.getContent());
//                System.out.println(aclMessage.getContent());
            }

            @Override
            public boolean done() {
                return it == GAUtils.MAX_IT || agentsFitness.get(0).getFitness() == GAUtils.MAX_FITNESS;
            }
        });
        addBehaviour(sequentialBehaviour);
    }
private void calculateFintness(){
    ACLMessage message=new ACLMessage(ACLMessage.REQUEST);

    for (AgentFitness agf:agentsFitness) {
        message.addReceiver(agf.getAid());
    }
    message.setContent("fitness");
    send(message);

}
private void setAgentFintess(AID aid,int fitness){
        for (int i=0;i<GAUtils.POPULATION_SIZE;i++){
            if(agentsFitness.get(i).getAid().equals(aid)){
                agentsFitness.get(i).setFitness(fitness);
//                System.out.println(fitness+"=:="+agentsFitness.get(i).getFitness());
                break;
            }
        }
}

private void sendMessage(AID aid, String content, int preformative){
    ACLMessage message=new ACLMessage(preformative);
    message.addReceiver(aid);
    message.setContent(content);
    send(message);
}
private void showPopulation(){
    for (AgentFitness agentFitness:agentsFitness) {
        System.out.println(agentFitness.getAid().getName()+" : "+agentFitness.getFitness());
    }
}
}
