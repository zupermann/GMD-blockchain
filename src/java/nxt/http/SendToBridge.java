package nxt.http;

import nxt.Account;
import nxt.NxtException;
import nxt.crypto.Crypto;
import nxt.util.Convert;
import nxt.util.ProxyInvocationHandler;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;

public class SendToBridge extends CreateTransaction{
    static final SendToBridge instance = new SendToBridge();

    private SendToBridge() {
        super(new APITag[] {APITag.ACCOUNTS, APITag.CREATE_TRANSACTION, APITag.TRANSACTIONS}, "bridgeAddress", "senderAddress", "amountNQT", "remoteNetwork", "remoteAddress");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        HttpServletRequest proxyRequest = ProxiedHttpServletRequest.getNewProxy(req);
        long bridgeAddress = ParameterParser.getAccountId(proxyRequest, "bridgeAddress", true);
        long amountNQT = ParameterParser.getAmountNQT(proxyRequest);
        Account account = ParameterParser.getSenderAccount(proxyRequest);
        checkValid(proxyRequest);

        //replacing HttpServletRequest with a proxy defined in ProxiedHttpServletRequest in order to intercep requests to "getParameter"
        return createTransaction(proxyRequest, account, bridgeAddress, amountNQT);
    }

    private void checkValid(HttpServletRequest req) throws ParameterException {
        String messageIsPrunable = req.getParameter("messageIsPrunable");
        if(messageIsPrunable != null && "true".equalsIgnoreCase(messageIsPrunable)) {
            throw new ParameterException(JSONResponses.INCORRECT_MOVE_PRUNABLE);
        }
        String destBridgeAddr = req.getParameter("bridgeAddress");
        if(destBridgeAddr==null) {
            throw new ParameterException(JSONResponses.incorrect("bridgeAddress"));
        }

        long accountId;
        try {
            accountId = Crypto.rsDecode(destBridgeAddr.substring(4));
        } catch (RuntimeException e){
            throw new ParameterException(JSONResponses.incorrect("bridgeAddress"));
        }
        Account account = Account.getAccount(accountId);
        final String remoteNetwork = req.getParameter("remoteNetwork");
        if (!account.getAccountInfo().getName().toLowerCase().startsWith(remoteNetwork)){
            throw new ParameterException(JSONResponses.incorrect("bridgeAddress",
                    "Make sure bridgeAddress and remoteNetwork point to the correct bridge. Name of the bridge should start with name of the network."));
        }

    }

    public static class ProxiedHttpServletRequest {

        public static HttpServletRequest getNewProxy(HttpServletRequest instance){
            return (HttpServletRequest)ProxyInvocationHandler.getNewProxy(HttpServletRequest.class, instance, triConsumer);
        }


        private static ProxyInvocationHandler.TriConsumer<Method,Object[],HttpServletRequest> triConsumer = (method,args,reqInstance)->{

            if("getParameter".equals(method.getName())){
                if(args!=null && args.length == 1 && args[0] instanceof String){
                    switch ((String)args[0]) {
                        case "message":
                            final String remoteNetwork = reqInstance.getParameter("remoteNetwork");
                            final String remoteAddress = reqInstance.getParameter("remoteAddress");
                            JSONObject json = new JSONObject();
                            json.put("remoteNetwork", remoteNetwork);
                            json.put("remoteAddress", remoteAddress);
                            String message = reqInstance.getParameter("message");
                            if(message!=null && !message.isEmpty()) {
                                json.put("descrString", message);
                            }
                            return json.toJSONString();//.replaceAll("\\", "");
//                        case "secretPhrase":
//                            return "";
                        case "publicKey":
                            try {
                                long accountId = ParameterParser.getAccountId(reqInstance, "senderAddress", true);
                                byte[] publicKey = Account.getPublicKey(accountId);
                                return Convert.toHexString(publicKey);
                            } catch (ParameterException e) {
                                e.printStackTrace();
                                return "";
                            }
                    }

                }
            }
            return null;
        };
    }
}
