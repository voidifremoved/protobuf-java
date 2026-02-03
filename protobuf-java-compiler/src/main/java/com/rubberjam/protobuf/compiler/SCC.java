package com.rubberjam.protobuf.compiler;

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FileDescriptor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

/**
 * Description of each strongly connected component. Note that the order of both the descriptors in
 * this SCC and the order of children is deterministic.
 */
public final class SCC
{
  public final List<Descriptor> descriptors = new ArrayList<>();
  public final List<SCC> children = new ArrayList<>();

  public Descriptor getRepresentative()
  {
    return descriptors.get(0);
  }

  // All messages must necessarily be in the same file.
  public FileDescriptor getFile()
  {
    return descriptors.get(0).getFile();
  }

  public boolean contains(Descriptor message)
  {
    return descriptors.contains(message);
  }

  public interface DepsGenerator
  {
    Iterable<Descriptor> getDeps(Descriptor descriptor);
  }

  public static class Analyzer
  {
    private final Map<Descriptor, NodeData> cache = new HashMap<>();
    private final Stack<Descriptor> stack = new Stack<>();
    private int index = 0;
    private final List<SCC> garbageBin = new ArrayList<>(); // To keep track of created SCCs
    private final DepsGenerator depsGenerator;

    public Analyzer(DepsGenerator depsGenerator)
    {
      this.depsGenerator = depsGenerator;
    }

    public SCC getSCC(Descriptor descriptor)
    {
      if (cache.containsKey(descriptor))
      {
        SCC scc = cache.get(descriptor).scc;
        if (scc != null)
        {
          return scc;
        }
        // If scc is null, it means we are currently visiting this node in the DFS stack.
        // But getSCC is usually called from outside.
        // If called from outside, cache should contain complete NodeData with SCC if visited.
        // If it is being visited, scc is null.
        // DFS handles this.
      }
      return dfs(descriptor).scc;
    }

    private static class NodeData
    {
      SCC scc; // if null it means it's still on the stack
      int index;
      int lowlink;
    }

    private SCC createSCC()
    {
      SCC scc = new SCC();
      garbageBin.add(scc);
      return scc;
    }

    private NodeData dfs(Descriptor descriptor)
    {
      NodeData result = new NodeData();
      result.index = result.lowlink = index++;
      cache.put(descriptor, result);
      stack.push(descriptor);

      for (Descriptor dep : depsGenerator.getDeps(descriptor))
      {
        if (dep == null) continue;

        NodeData childData = cache.get(dep);
        if (childData == null)
        {
          // unexplored node
          childData = dfs(dep);
          result.lowlink = Math.min(result.lowlink, childData.lowlink);
        }
        else
        {
          if (childData.scc == null)
          {
            // Still in the stack so we found a back edge
            result.lowlink = Math.min(result.lowlink, childData.index);
          }
        }
      }

      if (result.index == result.lowlink)
      {
        // This is the root of a strongly connected component
        SCC scc = createSCC();
        while (true)
        {
          Descriptor sccDesc = stack.pop();
          scc.descriptors.add(sccDesc);
          cache.get(sccDesc).scc = scc;
          if (sccDesc == descriptor) break;
        }

        // The order of descriptors is random and depends how this SCC was
        // discovered. In-order to ensure maximum stability we sort it by name.
        Collections.sort(scc.descriptors, new Comparator<Descriptor>()
        {
          @Override
          public int compare(Descriptor a, Descriptor b)
          {
            return a.getFullName().compareTo(b.getFullName());
          }
        });

        addChildren(scc);
      }
      return result;
    }

    private void addChildren(SCC scc)
    {
      Set<SCC> seen = new HashSet<>();
      for (Descriptor descriptor : scc.descriptors)
      {
        for (Descriptor childMsg : depsGenerator.getDeps(descriptor))
        {
          if (childMsg == null) continue;
          SCC child = getSCC(childMsg);
          if (child == scc) continue;
          if (seen.add(child))
          {
            scc.children.add(child);
          }
        }
      }
    }
  }
}
