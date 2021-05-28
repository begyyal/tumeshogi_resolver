package begyyal.shogi.processor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.math.NumberUtils;

import begyyal.commons.util.object.SuperList;
import begyyal.commons.util.object.SuperList.SuperListGen;
import begyyal.commons.util.object.Tree;
import begyyal.shogi.def.Koma;
import begyyal.shogi.def.Player;
import begyyal.shogi.object.Ban;
import begyyal.shogi.object.BanContext;
import begyyal.shogi.object.MasuState;

public class BattleProcessor {

    private final SuperList<BanContext> contexts;
    private final int numOfMoves;

    private BattleProcessor(String numStr, String banStr, String motigomaStr) {
	if (!NumberUtils.isParsable(numStr))
	    throw new IllegalArgumentException(
		"The argument of number of moves must be number format.");
	this.numOfMoves = Integer.parseInt(numStr);
	if (this.numOfMoves % 2 != 1)
	    throw new IllegalArgumentException(
		"The argument of number of moves must be odd number.");
	this.contexts = SuperListGen.of(BanContext.newi(banStr, motigomaStr));
    }

    /**
     * 詰将棋専用。
     * 
     * @return 回答
     */
    public String[] calculate() {

	var results = SuperListGen.<BanContext>newi();
	int count = 0;

	do {
	    for (BanContext acon : this.shallowCopyContexts())
		processSelf(acon);
	    count++;
	    if (this.contexts.isEmpty())
		break;
	    for (BanContext acon : this.shallowCopyContexts())
		processOpponent(acon, results, count);
	    count++;
	} while (!this.contexts.isEmpty() && count < numOfMoves);

	if (results.isEmpty())
	    return createFailureLabel();

	var result = this.selectContext(results);
	if (result == null)
	    return createFailureLabel();

	return this.summarize(result.log);
    }

    @SuppressWarnings("unchecked")
    private ArrayList<BanContext> shallowCopyContexts() {
	return (ArrayList<BanContext>) this.contexts.clone();
    }

    private String[] createFailureLabel() {
	return new String[] { "詰めませんでした。" };
    }

    private void processSelf(BanContext acon) {

	this.contexts.removeIf(c -> c.id == acon.id);

	var branches = SelfProcessor.newi().spread(acon);
	if (branches != null)
	    this.contexts.addAll(branches);
    }

    private void processOpponent(BanContext acon, SuperList<BanContext> results, int count) {

	this.contexts.removeIf(c -> c.id == acon.id);

	var branches = OpponentProcessor.newi().spread(acon);
	if (branches == null || branches.length == 0) {
	    results.add(acon);
	    return;
	} else if (count == numOfMoves) {
	    acon.isFailure = true;
	    results.add(acon);
	    return;
	}

	Arrays.stream(branches)
	    .filter(c -> {
		long selfBanCount = c.getLatestBan().search(s -> s.player == Player.Self).count();
		return selfBanCount > 0 && selfBanCount + c.selfMotigoma.size() > 1;
	    })
	    .forEach(this.contexts::add);
    }

    private String[] summarize(List<Ban> bans) {

	var tejun = SuperListGen.<MasuState>newi();

	Ban from = null;
	for (Ban to : bans) {
	    if (from != null)
		tejun.add(parseBanDiff(from, to));
	    from = to;
	}

	return tejun.stream()
	    .map(s -> writeItte(s))
	    .toArray(String[]::new);
    }

    private MasuState parseBanDiff(Ban from, Ban to) {
	return from.serializeMatrix()
	    .zip(to.serializeMatrix())
	    .stream()
	    .filter(p -> !p.getLeft().isEqualWithoutRange(p.getRight()))
	    .map(p -> p.getRight())
	    .filter(s -> s.koma != Koma.Empty)
	    .findFirst()
	    .get();
    }

    private String writeItte(MasuState state) {
	var sb = new StringBuilder();
	sb.append(state.player);
	sb.append(" -> ");
	sb.append(state.getSuzi());
	sb.append(state.getDan());
	sb.append(state.koma);
	if (state.nariFlag)
	    sb.append("Nari");
	return sb.toString();
    }

    private BanContext selectContext(SuperList<BanContext> results) {
	var resultTree = recursive4selectContext(context2tree(results).getChildren(), true);
	return resultTree == null
		? null
		: results
		    .stream()
		    .filter(c -> c.getLatestBan().id == resultTree.getValue())
		    .findFirst().get();
    }

    private Tree<Integer> recursive4selectContext(Set<Tree<Integer>> branches, boolean isSelf) {
	// self -> 詰み筋が多い方
	// opponent -> 深度が深い方。深度が同一の場合はその深度の分岐数が多い方

	Tree<Integer> result = null;
	long criterion = 0, criterion2 = 0, temp;

	for (var b : branches) {

	    if (CollectionUtils.isEmpty(b.getChildren()))
		return b;

	    var tips = b.collectTips();
	    if (!isSelf) {

		final long maxDepth = tips
		    .stream()
		    .map(t -> t.getDepth())
		    .sorted(Comparator.reverseOrder())
		    .findFirst().get();

		if (criterion == maxDepth
			&& criterion2 < (temp = tips
			    .stream()
			    .filter(t -> t.getDepth() == maxDepth)
			    .count())
			&& (criterion2 = temp) > 0
			|| criterion < maxDepth && (criterion = maxDepth) > 0)
		    result = b;

	    } else if (criterion < (temp = tips
		.stream()
		.map(t -> t.getDepth())
		.filter(d -> d % 2 == 1)
		.count())
		    && (criterion = temp) > 0)
		result = b;
	}

	return this.recursive4selectContext(result.getChildren(), !isSelf);
    }

    // 引数のコンテキストのログを集積して初期配置からの盤面の分岐をツリー化
    private Tree<Integer> context2tree(SuperList<BanContext> results) {

	var origin = Tree.convert(
	    results.get(0).log
		.stream()
		.map(b -> b.id)
		.collect(Collectors.toList()));

	results.subList(1, results.size())
	    .stream()
	    .map(c -> {
		var idList = c.log.subList(1, c.log.size())
		    .stream()
		    .map(b -> b.id)
		    .collect(Collectors.toList());
		if (c.isFailure)
		    idList.add(Ban.generateId());
		return idList;
	    })
	    .forEach(idList -> origin.compound(idList));

	return origin;
    }

    public static BattleProcessor of(String numStr, String banStr, String motigomaStr) {
	return new BattleProcessor(numStr, banStr, motigomaStr);
    }
}
