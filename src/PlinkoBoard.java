import java.io.*;

public class plinkoBoard {
    int[][] board;
    int x,y;
    public plinkoBoard(int x, int y){
        this.board = initializeGame(x,y);
        this.x = x;
        this.y = y;
    }


    /*
     * 0 == Able to drop ball, 1 == able to place peg, 2 == Active Peg, 3==ball
     */

    public int[][] initializeGame(int x, int y){
        int[][] out = new int[x][y];
        
        for(int i = 0; i < x; i ++){
            for(int j = 0; j < y; j++){
                if(Math.random() >= 0.3 && i>=2) out[i][j] = 2;
                else if(i<2) out[i][j] =0;
                else out[i][j] = 1;
            }
        }
        return out;
    }

    public String printBoard(int[][] board){
        String out = "";
        int x = board.length;
        int y = board[0].length;
        for(int i = 0; i < x; i++){
            for(int j = 0; j < y; j++){
                out += String.valueOf(this.board[i][j]);
            }
            out += "\n";
        }

        return out;
    }

    public void SeralizeBoard(String fileName){
        try{
            FileOutputStream file = new FileOutputStream(fileName);
            ObjectOutputStream out = new ObjectOutputStream(file); 
            out.writeObject(this.board);
            out.close();
        }
        catch(IOException ex){
            System.out.println("Oopsies couldn't serailize :(");
        }
    }

    public boolean dropBall(int col, int row) {

        if(col > 1)return false;
        
        while(col < x-1){
            int temp = this.board[col][row];
            int _col = col, _row = row;
            this.board[col][row]=3;
            System.out.println(board[col++][row]);
            if(board[col++][row] == 2){
                double rand = Math.random();
                if(board[col][row++]==2 & board[col][row--]==2) return false;
                else if(board[col][row++]!=2 && board[col][row--]==2) row++;
                else if(board[col][row++]==2 && board[col][row--]!=2) row--;
                else if(rand >= 0.5 & board[col][row++] != 2 & board[col][row--] !=2) row++;
                else if(rand < 0.5 & board[col][row++] != 2 & board[col][row--] !=2) row--;
            }
            else{
                col++;
            }

            //For testing
            System.out.println(printBoard(this.board));

            //reset previous tile
            this.board[_col][_row]=temp;
        }

        return true;
    }

        

    public static void main(String[] args){
        plinkoBoard board = new plinkoBoard(10,20);
        board.dropBall(1,5);
    }
}

