package nxt.http;

import nxt.Account;
import nxt.NxtException;
import nxt.crypto.Crypto;
import nxt.util.ProxyInvocationHandler;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;

public class MoveMoneyToExternalChain extends CreateTransaction{
    static final MoveMoneyToExternalChain instance = new MoveMoneyToExternalChain();

    private MoveMoneyToExternalChain() {
        super(new APITag[] {APITag.ACCOUNTS, APITag.CREATE_TRANSACTION}, "bridgeAddress", "amountNQT", "remoteNetwork", "remoteAddress");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        long bridgeAddress = ParameterParser.getAccountId(req, "bridgeAddress", true);
        long amountNQT = ParameterParser.getAmountNQT(req);
        final String remoteNetwork = req.getParameter("remoteNetwork");
        final String remoteAddress = req.getParameter("remoteAddress");
        Account account = ParameterParser.getSenderAccount(req);
        checkValid(req);

        ProxyInvocationHandler.TriConsumer<Method,Object[],HttpServletRequest> triConsumer = (method,args,reqInstance)->{
            if(method == null) {
                return null;
            }
            if("getParameter".equals(method.getName())){
                if(args!=null && args.length == 1 && args[0] instanceof String){
                    if( (args[0]).equals("message") ){
                        JSONObject json = new JSONObject();
                        json.put("remoteNetwork", remoteNetwork);
                        json.put("remoteAddress", remoteAddress);
                        String message = reqInstance.getParameter("message");
                        if(message!=null && !message.isEmpty()) {
                            json.put("descrString", message);
                        }
                        return json.toJSONString();//.replaceAll("\\", "");
                    }
                }
            }
            return null;
        };
        HttpServletRequest httpServletRequestProxy = (HttpServletRequest)ProxyInvocationHandler.getNewProxy(HttpServletRequest.class, req, triConsumer);

        return createTransaction(httpServletRequestProxy, account, bridgeAddress, amountNQT);
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
}
