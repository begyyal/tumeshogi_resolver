package begyyal.shogi.processor;

import java.util.Arrays;
import java.util.stream.Stream;

import begyyal.commons.util.matrix.MatrixResolver;
import begyyal.commons.util.object.SuperBool;
import begyyal.shogi.def.Koma;
import begyyal.shogi.def.Player;
import begyyal.shogi.object.Ban;
import begyyal.shogi.object.BanContext;
import begyyal.shogi.object.BanContext.BranchParam;
import begyyal.shogi.object.MasuState;

public class OpponentProcessor extends PlayerProcessorBase {

    public static final Player PlayerType = Player.Opponent;

    private OpponentProcessor() {
	super();
    }

    public BanContext[] spread(BanContext context) {

	var ban = context.getLatestBan();
	var opponentOu = this.getOpponentOu(ban);

	// 王手範囲から避ける(王による王手駒の取得含む)
	var cs1 = spreadMasuState(opponentOu, ban)
	    .filter(s -> !s.rangedBy
		.anyMatch(r -> ban.getState(r.getLeft(), r.getRight()).player != PlayerType))
	    .map(s -> {
		var newBan = ban.clone();
		var k = newBan.advance(opponentOu.x, opponentOu.y, s.x, s.y, false);
		var dest = newBan.getState(s.x, s.y);
		return new BranchParam(newBan, dest, k, PlayerType, true);
	    })
	    .filter(bp -> bp.latestState().rangedBy
		.stream()
		.map(r -> bp.latestBan().getState(r.getLeft(), r.getRight()))
		.filter(s -> s.player != PlayerType)
		.findFirst()
		.isEmpty())
	    .map(bp -> context.branch(bp));

	var outeArray = opponentOu.rangedBy
	    .stream()
	    .map(r -> ban.getState(r.getLeft(), r.getRight()))
	    .filter(s -> s.player != PlayerType)
	    .toArray(MasuState[]::new);
	if (outeArray.length > 1)
	    return cs1.toArray(BanContext[]::new);

	// 王手駒を取得する(王による取得は含まず)
	var outeState = outeArray[0];
	var cs2 = outeState.rangedBy
	    .stream()
	    .map(r -> ban.getState(r.getLeft(), r.getRight()))
	    .filter(s -> s.player == PlayerType && s.koma != Koma.Ou)
	    .flatMap(from -> {
		var tryNari = SuperBool.newi();
		return createBranchStream(outeState.y, from)
		    .filter(i -> tryNari.get()
			    || Ban.validateState(from.koma, outeState.x, outeState.y, PlayerType))
		    .mapToObj(i -> {
			var newBan = ban.clone();
			var k = newBan.advance(
			    from.x, from.y, outeState.x, outeState.y, tryNari.getAndReverse());
			var dest = newBan.getState(outeState.x, outeState.y);
			return new BranchParam(newBan, dest, k, PlayerType, true);
		    });
	    })
	    .filter(bp -> bp.latestBan().search(s -> this.isOpponentOu(s))
		.findFirst()
		.get().rangedBy
		    .stream()
		    .map(r -> bp.latestBan().getState(r.getLeft(), r.getRight()))
		    .filter(s -> s.player != PlayerType)
		    .findFirst()
		    .isEmpty())
	    .map(bp -> context.branch(bp));

	// 持ち駒を貼る
	var outeVector = opponentOu.getVectorTo(outeState);
	boolean outeIsNotLinear = Math.abs(outeVector.x()) == 1 || Math.abs(outeVector.y()) == 1;
	var cs3 = context.opponentMotigoma.isEmpty() || outeIsNotLinear
		? Stream.empty()
		: Arrays.stream(MatrixResolver.decompose(outeVector))
		    .filter(miniV -> !outeVector.equals(miniV))
		    .flatMap(v -> context.opponentMotigoma
			.stream()
			.distinct()
			.map(k -> {
			    var newBan = ban.clone();
			    int x = opponentOu.x + v.x(), y = opponentOu.y + v.y();
			    var s = newBan.deploy(k, x, y, PlayerType);
			    return s == MasuState.Invalid ? null
				    : new BranchParam(newBan, s, k, PlayerType, false);
			}))
		    .filter(bp -> bp != null)
		    .map(bp -> context.branch(bp));

	return Stream.concat(Stream.concat(cs1, cs2), cs3).toArray(BanContext[]::new);
    }

    @Override
    protected Player getPlayerType() {
	return PlayerType;
    }

    public static OpponentProcessor newi() {
	return new OpponentProcessor();
    }
}
