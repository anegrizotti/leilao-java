package Servidor;

import static Criptografia.ModuloAES.algoritmo;
import static Criptografia.ModuloAES.Decriptografa;
import static Criptografia.ModuloAES.Criptografa;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.Socket;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.text.BadLocationException;

public class ComunicacaoServer extends Thread {

    public static final String ip = "230.0.0.0";
    public static final Integer porta = 4321;
    public static ServidorGUI InterfaceServidor;
    public String nomeItem = "";
    public String valorItem = "";
    public String Tempo = "";
    
    static SecretKeySpec secKeySpec;
    static IvParameterSpec ivParamSpec;

    public ComunicacaoServer(ServidorGUI Novo) throws IOException, ClassNotFoundException {
        InterfaceServidor = Novo;
    }

    public void MandaLance(String[] message) throws IOException, ClassNotFoundException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        String aux = "";
        for (String i : message) {
            aux += i;
        }

        DatagramSocket socket = new DatagramSocket();
        InetAddress group = InetAddress.getByName(ip);
        System.out.println("Mensagem Enviada: " + aux);

        String cipherText = Criptografa(algoritmo, aux, secKeySpec, ivParamSpec);
        byte[] cipherTextBytes = cipherText.getBytes();

        DatagramPacket packet = new DatagramPacket(cipherTextBytes, cipherTextBytes.length, group, porta);
        socket.send(packet);
        socket.close();
    }

    public void RecebeLance() throws IOException, BadLocationException, ClassNotFoundException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        byte[] buffer = new byte[1024];
        MulticastSocket socket = new MulticastSocket(porta);
        InetAddress group = InetAddress.getByName(ip);
        socket.joinGroup(group);

        while (true) {
            System.out.println("Esperando Mensagens...");
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);

            String msgCriptografada = new String(packet.getData(), packet.getOffset(), packet.getLength());
            String msg = null;
            try {
                msg = Decriptografa(algoritmo, msgCriptografada, secKeySpec, ivParamSpec);
            } catch (Exception e) {
            }

            System.out.println("Mensagem Recebida: " + msg);

            if (!msg.isEmpty() && !msg.startsWith("Nome")) {
                String[] message = {"Nome: " + this.nomeItem + " Valor item: " + this.valorItem + " Tempo: " + this.Tempo};
                MandaLance(message);
            }
        }
    }

    private static void RecebeChaveCriptografada() throws NoSuchAlgorithmException, IOException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException {
        KeyPairGenerator gerador;
        gerador = KeyPairGenerator.getInstance("RSA");

        gerador.initialize(2048);
        KeyPair parDeChaves = gerador.generateKeyPair();

        PublicKey chavePublica = parDeChaves.getPublic();
        PrivateKey chavePrivada = parDeChaves.getPrivate();

        Socket socket = new Socket("localhost", 3333);
        DataInputStream input = new DataInputStream(socket.getInputStream());
        DataOutputStream output = new DataOutputStream(socket.getOutputStream());

        byte[] chavePublicaBytes;
        byte[] chaveSimetricaBytes = null;
        byte[] vetorInicializacaoBytes = null;

        chavePublicaBytes = chavePublica.getEncoded();

        output.writeInt(chavePublicaBytes.length);
        output.write(chavePublicaBytes);
        output.flush();

        Cipher objCifra;
        objCifra = Cipher.getInstance("RSA");

        objCifra.init(Cipher.DECRYPT_MODE, chavePrivada);

        int length = input.readInt();                    
        if (length > 0) {
            chaveSimetricaBytes = new byte[length];
            input.readFully(chaveSimetricaBytes, 0, chaveSimetricaBytes.length); 
        }

        int length2 = input.readInt();                  
        if (length2 > 0) {
            vetorInicializacaoBytes = new byte[length];
            input.readFully(vetorInicializacaoBytes, 0, vetorInicializacaoBytes.length); 
        }

        byte[] mensagemDecriptografadaBytes = objCifra.doFinal(chaveSimetricaBytes);
        byte[] vetorDecriptografadoBytes = objCifra.doFinal(vetorInicializacaoBytes);

        secKeySpec = new SecretKeySpec(mensagemDecriptografadaBytes, "AES");
        ivParamSpec = new IvParameterSpec(vetorDecriptografadoBytes);

        socket.close();
    }

    @Override
    public void run() {
        try {
            RecebeChaveCriptografada();
        } catch (Exception e) {
        }

        try {
            RecebeLance();
        } catch (Exception ex) {
        }
    }
}
