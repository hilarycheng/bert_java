Bert for Java - Java Library for Binary Erlang Term
=============

Encoding / Decoding is Supported, It has been tested using data against with Erlang R16B03-1.

- Small Integer (97)
- Integer       (98)
- Floating      (99)
- Atom          (100)
- Small Tuple   (104)
- Large Tuple   (105)
- Nil           (106)
- String        (107)
- List          (108)
- Binary        (109)
- Bert Time     ( Will be Converted to Millisecond Rather MicroSecond )
- Bert True
- Bert False
- Bert Nil
- Bert Dict     ( Tested Against with R16B03-1, erlange term { age, 30 } in dict will be convert to { age, [ 30 ] }. It is done by Erlang itself )

Target
----
- Basic Support Erlang Term
- Except Regular Expression 
- First Version will not supported compression term yet

You can get more information 
- Specification : http://bert-rpc.org/ 
- Erlang Term : http://erlang.org/doc/apps/erts/erl_ext_dist.html

