package com.orientechnologies.orient.core.sql.executor;

import com.orientechnologies.common.concur.OTimeoutException;
import com.orientechnologies.orient.core.command.OCommandContext;

import java.util.Map;
import java.util.Optional;

/**
 * @author Luigi Dell'Aquila
 */
public class MatchStep extends AbstractExecutionStep {
  protected final EdgeTraversal edge;

  OTodoResultSet     upstream;
  OResult            lastUpstreamRecord;
  MatchEdgeTraverser traverser;
  OResult            nextResult;

  public MatchStep(OCommandContext context, EdgeTraversal edge) {
    super(context);
    this.edge = edge;
  }

  @Override public OTodoResultSet syncPull(OCommandContext ctx, int nRecords) throws OTimeoutException {
    return new OTodoResultSet() {
      int localCount = 0;

      @Override public boolean hasNext() {
        if (localCount >= nRecords) {
          return false;
        }
        if (nextResult == null) {
          fetchNext(ctx, nRecords);
        }
        if (nextResult == null) {
          return false;
        }
        return true;
      }

      @Override public OResult next() {
        if (localCount >= nRecords) {
          throw new IllegalStateException();
        }
        if (nextResult == null) {
          fetchNext(ctx, nRecords);
        }
        if (nextResult == null) {
          throw new IllegalStateException();
        }
        OResult result = nextResult;
        fetchNext(ctx, nRecords);
        localCount++;
        return result;
      }

      @Override public void close() {

      }

      @Override public Optional<OExecutionPlan> getExecutionPlan() {
        return null;
      }

      @Override public Map<String, Object> getQueryStats() {
        return null;
      }
    };
  }

  private void fetchNext(OCommandContext ctx, int nRecords) {
    nextResult = null;
    while (true) {
      if (upstream == null || !upstream.hasNext()) {
        upstream = getPrev().get().syncPull(ctx, nRecords);
      }
      if (!upstream.hasNext()) {
        return;
      }

      lastUpstreamRecord = upstream.next();
      traverser = new MatchEdgeTraverser(lastUpstreamRecord, edge);

      if (traverser.hasNext(ctx)) {
        nextResult = traverser.next(ctx);
        break;
      }
    }
  }

  @Override public void asyncPull(OCommandContext ctx, int nRecords, OExecutionCallback callback) throws OTimeoutException {

  }

  @Override public void sendResult(Object o, Status status) {

  }

  @Override public String prettyPrint(int depth, int indent) {
    String spaces = OExecutionStepInternal.getIndent(depth, indent);
    StringBuilder result = new StringBuilder();
    result.append(spaces);
    result.append("+ MATCH \n");
    result.append(spaces);
    result.append("  ");
    if(edge.out) {
      result.append("{"+edge.edge.out.alias+"}");
      result.append(edge.edge.item.getMethod());
      result.append("{"+edge.edge.in.alias+"}");
    }else{
      result.append("{"+edge.edge.in.alias+"}");
      result.append(edge.edge.item.getMethod());
      result.append("{"+edge.edge.out.alias+"}");
    }
    return result.toString();
  }
}