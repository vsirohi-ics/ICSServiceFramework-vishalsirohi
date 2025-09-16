package com.ics.reef.devtools;

import com.ics.reef.api.interfaces.ReefRouter;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;

import java.util.Set;

/**
 * Netty handler for Smithy operations explorer.
 * Only active in development environments.
 */
public class ExplorerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    
    private final ReefRouter router;
    
    public ExplorerHandler(ReefRouter router) {
        this.router = router;
    }
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
        if (request.uri().equals("/explorer") || request.uri().equals("/explorer/")) {
            handleExplorer(ctx);
        } else {
            // Pass to next handler in pipeline
            ctx.fireChannelRead(request.retain());
        }
    }
    
    private void handleExplorer(ChannelHandlerContext ctx) {
        Set<String> operations = router.getSupportedOperations();
        
        String html = """
            <!DOCTYPE html><html><head>
            <title>🪸 Reef Smithy Explorer</title>
            <style>
            body { font-family: system-ui; margin: 40px; background: #f8f9fa; }
            .container { max-width: 800px; margin: 0 auto; background: white; padding: 30px; border-radius: 8px; }
            h1 { color: #2c3e50; }
            .op { background: #f1f3f4; padding: 15px; margin: 10px 0; border-radius: 5px; }
            textarea { width: 100%%; height: 80px; font-family: monospace; }
            button { background: #1976d2; color: white; padding: 8px 16px; border: none; border-radius: 4px; }
            .resp { background: #263238; color: #eceff1; padding: 10px; margin-top: 10px; font-family: monospace; white-space: pre-wrap; }
            </style></head><body>
            <div class='container'>
            <h1>🪸 Reef Smithy Explorer</h1>
            <p>Operations: %d | Environment: Development</p>
            %s
            <script>
            async function test(op) {
              const req = document.getElementById('req-' + op).value;
              const resp = document.getElementById('resp-' + op);
              resp.style.display = 'block';
              resp.textContent = 'Testing...';
              try {
                const r = await fetch('/' + op, {
                  method: 'POST',
                  headers: {'Content-Type': 'application/json'},
                  body: req
                });
                resp.textContent = await r.text();
              } catch (e) {
                resp.textContent = 'Error: ' + e.message;
              }
            }
            </script>
            </div></body></html>
            """.formatted(operations.size(), generateOperations(operations));
        
        FullHttpResponse response = new DefaultFullHttpResponse(
            HttpVersion.HTTP_1_1, 
            HttpResponseStatus.OK,
            Unpooled.copiedBuffer(html, CharsetUtil.UTF_8)
        );
        
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        
        ctx.writeAndFlush(response);
    }
    
    private String generateOperations(Set<String> operations) {
        StringBuilder html = new StringBuilder();
        for (String op : operations) {
            html.append("<div class='op'>");
            html.append("<h3>").append(op).append("</h3>");
            html.append("<textarea id='req-").append(op).append("' placeholder='JSON request'>{}</textarea><br>");
            html.append("<button onclick=\"test('").append(op).append("')\">Test</button>");
            html.append("<div id='resp-").append(op).append("' class='resp' style='display:none'></div>");
            html.append("</div>");
        }
        return html.toString();
    }
}
