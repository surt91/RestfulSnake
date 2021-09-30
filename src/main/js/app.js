import {idx2color} from "./color";

const React = require('react');
const ReactDOM = require('react-dom');
import {
    Container,
    TextField,
    Grid,
    Box,
    TableContainer,
    Table, TableHead, TableRow, TableBody, TableCell, Paper
} from '@material-ui/core';
import {registerStompPromise} from "./websocket-listener";
import {registerKeyPresses, registerTouch} from "./registerEvents";
import Canvas from "./canvas";
import {draw} from "./canvasDraw";


class App extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            scale: 20,
            foodColor: "#cc2200",
            bgColor: "#000",
            game: {
                width: 20,
                height: 20,
                food: {x: -1, y: -1},
                snakes: [],
            },
            highscores: [],
            idx: -1
        };

        this.updateGameState = this.updateGameState.bind(this);
        this.updateHighscore = this.updateHighscore.bind(this);
        this.updateIdentity = this.updateIdentity.bind(this);
        this.handleKeydown = this.handleKeydown.bind(this);
    }

    componentDidMount() {
        this.init();
        registerKeyPresses(this.handleKeydown);
        // registerTouch();
    }

    init() {
        // check if we are joining an existing game, or starting a new one
        // https://stackoverflow.com/a/901144
        const urlSearchParams = new URLSearchParams(window.location.search);
        const params = Object.fromEntries(urlSearchParams.entries());

        const id = params["id"];

        if(id === undefined) {
            fetch(`/api/init/${this.state.game.width}/${this.state.game.height}`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                }
            }).then(response => response.json())
                .then((x) => {
                    this.join(x.id);
                });
        } else {
            this.join(id);
        }
    }

    join(id) {
        const url = window.location.origin + `?id=${id}`;

        this.setState({
            shareUrl: url,
            id: id
        });

        this.stompClientPromise = registerStompPromise([
            {route: '/topic/update/' + id, callback: this.updateGameState},
            {route: '/topic/newHighscore', callback: this.updateHighscore},
            {route: '/user/queue/getIdx', callback: this.updateIdentity},
        ]).then(x => {
            x.send("/app/join", {}, id);
            return x;
        });
    }

    move(dir) {
        this.stompClientPromise.then(x => x.send("/app/move", {}, JSON.stringify(dir)));
    }

    reset() {
        this.stompClientPromise.then(x => x.send("/app/reset", {}, ""));
    }

    unpause() {
        if(this.state.game.paused) {
            this.stompClientPromise.then(x => x.send("/app/unpause", {}, ""));
        }
    }

    pause() {
        if(!this.state.game.paused) {
            this.stompClientPromise.then(x => x.send("/app/pause", {}, ""));
        }
    }

    toggle_pause() {
        if(this.state.game.paused) {
            this.unpause();
        } else {
            this.pause();
        }
    }

    updateGameState(message) {
        const gameState = JSON.parse(message.body);
        this.setState({
            game: gameState
        });
    }

    updateHighscore(message) {
        if(message === undefined) {
            return;
        }

        const highscores = JSON.parse(message.body);
        this.setState({highscores: highscores});
    }

    updateIdentity(message) {
        console.log("my identity:", message.body);
        const ownIdx = message.body;
        this.setState({idx: ownIdx});
    }

    handleKeydown(e) {
        switch (e.code) {
            case "ArrowUp":
            case "KeyW":
                this.move("up");
                this.unpause();
                break;
            case "ArrowDown":
            case "KeyS":
                this.move("down");
                this.unpause();
                break;
            case "ArrowLeft":
            case "KeyA":
                this.move("left");
                this.unpause();
                break;
            case "ArrowRight":
            case "KeyD":
                this.move("right");
                this.unpause();
                break;
            case "KeyP":
                this.toggle_pause();
                break;
            case "KeyR":
                this.reset();
                break;
        }
    }

    handleNameChange(newName) {
        console.log("new name", newName);
    }

    //<!-- TODO share link -->

    render() {
        const options = {
            scale: this.state.scale,
            bgColor: this.state.bgColor,
            foodColor: this.state.foodColor
        }
        const scores = this.state.game.snakes.map(snake => {
            return {
                idx: snake.idx,
                playerName: snake.name,
                score: snake.length,
                color: idx2color(snake.idx)
            }
        })

        return (
            <Container maxWidth="lg">
                <Grid container spacing={2}>
                    <Grid item xs={8}>
                        <Canvas
                            draw={ctx => draw(ctx, this.state.game, options)}
                            width={this.state.game.width * this.state.scale}
                            height={this.state.game.height * this.state.scale}
                        />
                    </Grid>
                    <Grid item xs={4}>
                        <Scores
                            key="Scores"
                            title="Scores"
                            scores={scores}
                            editableIdx={this.state.idx}
                            onChange={this.handleNameChange}
                        />
                        <Scores
                            key="Highscores"
                            title="Highscores"
                            scores={this.state.highscores}
                        />
                    </Grid>
                </Grid>


            </Container>
        )
    }
}

class PlayerName extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            editMode: false
        }
    }

    render() {

    }
}

class Scores extends React.Component {
    constructor(props) {
        super(props);
    }

    render() {
        console.log(this.props)
        const fields = this.props.scores.map((score, index) =>
            <TableRow key={score.name + index}>
                <TableCell>
                    <Box sx={{
                        width: 20,
                        height: 20,
                        bgcolor: score.color
                    }}/>
                </TableCell>
                <TableCell>
                    {score.playerName}
                </TableCell>
                <TableCell>
                    {score.score}
                </TableCell>
            </TableRow>
        );

        return (
            <>
            <h2>{this.props.title}</h2>
            <TableContainer component={Paper}>
                <Table aria-label={this.props.title}>
                    <TableBody>
                        {fields}
                    </TableBody>
                </Table>
            </TableContainer>
            </>
        )
    }
}

ReactDOM.render(
    <App />,
    document.getElementById('react')
)