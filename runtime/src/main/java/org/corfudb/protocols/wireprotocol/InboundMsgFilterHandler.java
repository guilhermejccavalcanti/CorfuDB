package org.corfudb.protocols.wireprotocol;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Represent a Netty handler in charge of applying filters on the inbound
 * Corfu messages in order to transform them. The transformation are supplied
 * to this handler as a {@link List} of {@link MsgHandlingFilter}s which will
 * be applied in order. An example of such transforming filters is drop certain
 * messages.
 *
 * Created by Sam Behnam on 5/18/18.
 */
@Slf4j
public class InboundMsgFilterHandler extends ChannelDuplexHandler {

    private final List<MsgHandlingFilter> handlingFilters;

    public InboundMsgFilterHandler() {
        this(Collections.emptyList());
        log.info("InboundMsgFilterHandler is initialized without any " +
                "initial filters.");
    }

    public InboundMsgFilterHandler(@NonNull List<MsgHandlingFilter> handlingFilters) {
        this.handlingFilters = handlingFilters;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        CorfuMsg corfuMsg = (CorfuMsg) msg;

        List<CorfuMsg> transformedByFilters = applyFilters(corfuMsg);
        log.info("Applied {} filtering rule(s) on the inbound message. " +
                        "Message id:{} and Message type:{}",
                handlingFilters.size(),
                corfuMsg.getRequestID(),
                corfuMsg.getMsgType());

        for (CorfuMsg aCorfuMsg : transformedByFilters) {
            super.channelRead(ctx, aCorfuMsg);
        }
    }

    /** This method applies the collection of filters on the provided message
     * and return a list of transformed messages
     *
     * @param corfuMsg initial {@link CorfuMsg} to be transformed by filters
     * @return a {@link List} of {@link CorfuMsg}s generated by transforming
     *         input message.
     */
    private List<CorfuMsg> applyFilters(CorfuMsg corfuMsg) {
        List<CorfuMsg> results = Collections.singletonList(corfuMsg);
        for(MsgHandlingFilter filter : handlingFilters) {
            results = results.stream()
                    .map(filter)
                    .flatMap(List::stream)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }
        return results;
    }
}
