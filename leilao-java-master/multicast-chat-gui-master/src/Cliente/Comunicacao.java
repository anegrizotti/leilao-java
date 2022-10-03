package Cliente;

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
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.text.BadLocationException;
import static Criptografia.ModuloAES.algoritmo;
import static Criptografia.ModuloAES.Criptografa;
import static Criptografia.ModuloAES.Decriptografa;

public class Comunicacao extends Thread {

    public static final String ip = "230.0.0.0";
    public static final Integer porta = 4321;
    public static ClienteGUI ClienteInterface;

    static SecretKeySpec secKeySpec;
    static IvParameterSpec ivParamSpec;

    public Comunicacao(ClienteGUI NovoCliente) throws IOException, ClassNotFoundException {
        ClienteInterface = NovoCliente;
    }

    public void MandaLance(String[] message) throws IOException, ClassNotFoundException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        String lance = "";
        for (String i : message) {
            lance += i;
        }

        DatagramSocket socket = new DatagramSocket();
        InetAddress group = InetAddress.getByName(ip);
        System.out.println("Mensagem Enviada: " + lance);

        String cipherText = Criptografa(algoritmo, lance, secKeySpec, ivParamSpec);
        byte[] cipherTextBytes = cipherText.getBytes();

        DatagramPacket packet = new DatagramPacket(cipherTextBytes, cipherTextBytes.length, group, porta);
        socket.send(packet);
        socket.close();
    }

    public void RecebeLance() throws IOException, BadLocationException, ClassNotFoundException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        byte[] buffer = new byte[1024];
        MulticastSocket socket = new MulticastSocket(porta);
        InetAddress grupo = InetAddress.getByName(ip);
        socket.joinGroup(grupo);

        while (true) {
            System.out.println("Esperando Mensagens...");
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);

            String lanceCriptografado = new String(packet.getData(), packet.getOffset(), packet.getLength());
            String lance = null;
            try {
                lance = Decriptografa(algoritmo, lanceCriptografado, secKeySpec, ivParamSpec);
            } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidAlgorithmParameterException | InvalidKeyException | BadPaddingException | IllegalBlockSizeException ex) {
                Logger.getLogger(Comunicacao.class.getName()).log(Level.SEVERE, null, ex);
            }

            System.out.println("Mensagem Recebida: " + lance);
            
            if (lance.startsWith("Nome")) {
                ClienteInterface.PreencherInformacoesItem(lance);
            } else {
                ClienteInterface.AgregarLances(lance);
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
        } catch (NoSuchAlgorithmException | IOException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException | InvalidAlgorithmParameterException ex) {
            Logger.getLogger(Comunicacao.class.getName()).log(Level.SEVERE, null, ex);
        }

        try {
            RecebeLance();
        } catch (Exception ex) {
            Logger.getLogger(Comunicacao.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
