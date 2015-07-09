
var UpdatingCell = React.createClass({
    getInitialState: function() {
        return {isUpdating: false}
    },
    render: function () {
        var cname = this.state.isUpdating ? "value-color change" : "value-color"
        return <td className={cname}>{this.props.value}</td>
    },
    shouldComponentUpdate: function(nextProps, nextState){
        return nextProps.value != this.props.value || nextState.isUpdating != this.state.isUpdating
    },
    componentDidUpdate: function(prevProps, prevState) {
        if(!this.state.isUpdating && !prevState.isUpdating){
            var elm = $(this.getDOMNode())
            elm.one('animationend webkitAnimationEnd MSAnimationEnd oAnimationEnd', function(){
                this.setState({isUpdating : false})
            }.bind(this))
            this.setState({isUpdating : true})
        }
    }
})

var ChangeUpdatingCell = React.createClass({
    render: function(){

        if(this.props.value == 0){
            return <td className={'value-color'}>-</td>
        }

        var iconClass = 'glyphicon glyphicon-triangle-top',
            changeClass = 'change-color-positive';
        if(this.props.value < 0){
            iconClass = 'glyphicon glyphicon-triangle-bottom';
            changeClass = 'change-color-negative'
        }

        return <td className={changeClass}>{this.props.value} <span className={iconClass} aria-hidden="true"></span></td>
    }
})

var MarketDataRow = React.createClass({
    render: function () {
        var columnInputs = Object.keys(this.props.columnMappings).reduce(function(prev,colKey) {
            var column = this.props.columnMappings[colKey];
            if(column.show){
                var value = this.props.instrument[colKey]
                if(colKey === 'change'){
                    prev.push(<ChangeUpdatingCell value={this.props.instrument.change}/>)
                } else{
                    prev.push( column.dynamic ? <UpdatingCell value={value}/> : <td className="security-color">{value}</td> )
                }
            }
            return prev;
        }.bind(this), [])

        return (<tr>{columnInputs}</tr>);
    }
});

var MarketDataConfig = React.createClass({
    handleConfigUpdate: function(field, fieldKey, event){
        var columnsCopy = JSON.parse(JSON.stringify(this.props.columns))
        columnsCopy[fieldKey][field] = event.target.value
        this.props.configUpdate(columnsCopy)
    },
    handleCheckedUpdate: function(field, fieldKey, value){
        var columnsCopy = JSON.parse(JSON.stringify(this.props.columns))
        columnsCopy[fieldKey][field] = value
        this.props.configUpdate(columnsCopy)
    },
    render: function(){
        var configClass = this.props.showConfig ? "config-screen show-config" : "config-screen"

        var columnInputs = Object.keys(this.props.columns).reduce(function(prev,colKey){
            var column = this.props.columns[colKey];
            var checked = column.show ? <input type="checkbox" checked onChange={this.handleCheckedUpdate.bind(this,'show', colKey, false)}/> :
                                        <input type="checkbox" onChange={this.handleCheckedUpdate.bind(this,'show',colKey, true)}/>

            prev[colKey] =  (
              <div className="form-group">
                <label htmlFor="column-{column}" className="col-sm-2 control-label" >{colKey}</label>
                <div className="col-sm-4">
                    <input id="column-{column}" className="form-control input-sm" type="text" value={column.name} onChange={this.handleConfigUpdate.bind(this,'name', colKey)}/>
                </div>
                <div className="col-sm-1">
                    <div className="checkbox">
                        {checked}
                    </div>
                </div>
              </div>
            )

            return prev;
        }.bind(this), {})

        return (
          <div className={configClass}>
            <button className="close pull-right" onClick={this.props.cancelConfig}><i className="glyphicon glyphicon-remove"></i></button>
            <form className="form-horizontal">
            {columnInputs}
            </form>
          </div>
        )
    }
})

var MarketDataTable = React.createClass({
    getInitialState: function() {
        var instruments = {};
        feed.watch(this.props.watch, function(instrument) {
            console.log("instrument received " + instrument)
            instruments[instrument.data.symbol] = instrument.data;
            this.setState({instruments: instruments});
        }.bind(this));

        var columnMappings = this.props.columns.reduce(function(prev, item){
            prev[item.name] = {
                name : item.name,
                show: true,
                dynamic : item.dynamic
            }
            return prev;
        }, {})

        return {instruments: instruments, showConfig: false, columnMappings : columnMappings};
    },
    handleConfigUpdate : function(newConfigMapping){
        this.setState({ columnMappings : newConfigMapping})
    },
    toggleConfig: function(){
        this.setState({showConfig : !this.state.showConfig})
    },
    filterRows: function(event){
        console.log(event.target.value)
        this.setState({filter : event.target.value})
    },
    render: function () {
        var items = [];
        for (var symbol in this.state.instruments) {
            if(!this.state.filter || symbol.toUpperCase().includes( this.state.filter.toUpperCase() )) {
                var instrument = this.state.instruments[symbol];
                items.push(<MarketDataRow key={instrument.symbol} instrument={instrument} columnMappings={this.state.columnMappings}/>);
            }
        }

        var columns = this.props.columns.reduce(function(prev, column){
            var columnMapping = this.state.columnMappings[column.name]
            if(columnMapping.show){
                prev.push(<td className="value-color">{columnMapping.name}</td>)
            }

            return prev;
        }.bind(this), [])


        return (
          <div className="panel panel-default market-data">
              <MarketDataConfig columns={this.state.columnMappings} showConfig={this.state.showConfig} cancelConfig={this.toggleConfig} configUpdate={this.handleConfigUpdate}/>
              <div className="panel-body market-data-header">
                    <div className="col-xs-6 search-box">
                        <div className="inner-addon right-addon">
                            <i className="glyphicon glyphicon-search"></i>
                            <input className="form-control input-sm" type="text" onChange={this.filterRows}> </input>
                        </div>
                    </div>
                    <div className="col-xs-offset-5 col-xs-1">
                        <button className="btn btn-link btn-sm" onClick={this.toggleConfig}><i className="glyphicon glyphicon-cog"></i></button>
                    </div>
              </div>

            <table className="table table-condensed">
                <thead>
                    <tr>
                        {columns}
                    </tr>
                </thead>
                <tbody>
                    {items}
                </tbody>
            </table>
          </div>
        );
    }
});

var columns = [{name: "symbol", dynamic: false},
        {name:"bid", dynamic: true},
        {name:"ask", dynamic: true},
        {name:"volume", dynamic: true}]

var watch = ["algotrader.marketdata"]


React.render(<MarketDataTable columns={columns} watch={watch}/>, document.getElementById('main'));