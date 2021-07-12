- **品質担保**  
[![Java CI with Gradle](https://github.com/begyyal/tumeshogi_resolver/actions/workflows/ci.yml/badge.svg)](https://github.com/begyyal/tumeshogi_resolver/actions/workflows/ci.yml)
- **開発補助**  
[![Create feature branch](https://github.com/begyyal/tumeshogi_resolver/actions/workflows/create-feature.yml/badge.svg)](https://github.com/begyyal/tumeshogi_resolver/actions/workflows/create-feature.yml)  
[![Delete feature branch](https://github.com/begyyal/tumeshogi_resolver/actions/workflows/delete-feature.yml/badge.svg)](https://github.com/begyyal/tumeshogi_resolver/actions/workflows/delete-feature.yml)  
[![Post processing of the PR](https://github.com/begyyal/tumeshogi_resolver/actions/workflows/closed-pr.yml/badge.svg)](https://github.com/begyyal/tumeshogi_resolver/actions/workflows/closed-pr.yml)

# 概要

abc詰将棋を解くプログラムです。  
盤面及び持ち駒のデータを引数として渡すことで、  
詰み筋が存在する場合はその手筋を、そうでない場合は詰めない旨を標準出力します。  

# 引数のフォーマット

- 第1引数 : 手数 (〇手詰み)

- 第2引数 : 盤面  
座標 + プレイヤ種別 + 駒種別 (+成りの場合はフラグを追加) (駒の数分繰り返し)

- 第3引数 : 持ち駒 ※オプション。無し可。  
プレイヤ種別 + 駒種別 + 枚数 (種類数分繰り返し) ... + プレイヤ種別 + 駒種別 + 枚数 (種類数分繰り返し)

- ※上記に割り振る文字
  - プレイヤ種別
    - 攻め手 : x
    - 受け手 : y
  - 座標
    - 通常の棋譜読み通り、筋/段を数字にする。(e.g.) 一三 -> 13
  - 駒種別
    - 歩 : a	
    - 香 : b
    - 桂 : c
    - 銀 : d
    - 金 : e
    - 角 : f
    - 飛 : g
    - 王 : h
  - 成りフラグ : z
  
- 例
  - その1 (1手詰め)  
    第1引数 : 1  
    第2引数 : 31xfz11yb12yh  
    第3引数 : xe1ya18b3c4d4e3f1g2  
  - その2 (7手詰め)  
    第1引数 : 7  
    第2引数 : 31xfz11yb12yh  
    第3引数 : xe1ya18b3c4d4e3f1g2  
