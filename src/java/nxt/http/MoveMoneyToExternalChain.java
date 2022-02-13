package nxt.http;

import nxt.Account;
import nxt.NxtException;
import nxt.util.ProxyInvocationHandler;
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

        ProxyInvocationHandler.TriConsumer triConsumer = (t,u,v)->{
            Method m = (Method) t;
            Object[] args = (Object[]) u;
            HttpServletRequest reqInstance = (HttpServletRequest) v;
            if(m == null) {
                return null;
            }

            if("getParameter".equals(m.getName())){
                if(args!=null && args.length == 1 && args[0] instanceof String){
                    if( (args[0]).equals("message") ){
                        return "{\"remoteNetwork\": \""+remoteNetwork+"\", \"remoteAddress\": \""+remoteAddress+"\", \"descString\": \""+ reqInstance.getParameter("message")+"\"";
                    }
                }
            }


            return null;
        };
        HttpServletRequest httpServletRequestProxy = (HttpServletRequest)ProxyInvocationHandler.getNewProxy(HttpServletRequest.class, req, triConsumer);


//        if ("true".equalsIgnoreCase(req.getParameter("messageIsPrunable"))) {
//            //TODO
//        } else {
//            message = (Appendix.Message) ParameterParser.getPlainMessage(req, false);
//        }

        return createTransaction(httpServletRequestProxy, account, bridgeAddress, amountNQT);
    }
}
