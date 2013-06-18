$ ->
    class Enquetes
        @findByAdminKey: (adminKey) ->
            $.ajax
                url: "/admin/#{adminKey}"
                type: 'get'
                dataType: 'json'
            .pipe(Enquetes.fromAPIdata)
        @create: (enquete) ->
            $.ajax
                url: '/'
                type: 'post'
                contentType: 'application/json; Charset=UTF-8'
                data: JSON.stringify(Enquetes.toAPIdata(enquete))
                dataType: 'json'
        @update: (adminKey, enquete) ->
            $.ajax
                url: "/admin/#{adminKey}"
                type: 'post'
                contentType: 'application/json; Charset=UTF-8'
                data: JSON.stringify(Enquetes.toAPIdata(enquete))
        @fromAPIdata: (d) ->
            d.enquete.questions.forEach (q) -> q.questionType = QuestionType[q.questionType]
            new Enquete(d.enquete)
        @toAPIdata: (enquete) ->
            d = ko.toJS(enquete)
            d.questions.forEach (q) -> q.questionType = q.questionType.name
            d

    class QuestionOption
        constructor: (qo) ->
            @id = qo?.id
            @description = ko.observable(qo?.description)
            @valid = ko.computed(@validate)
        validate: =>
            Boolean(@description())

    class Question
        constructor: (q) ->
            @id = q?.id
            @description = ko.observable(q?.description)
        @create: (q) ->
            Question.constructorOf(q.questionType)(q)
        @constructorOf: (questionType) ->
            switch questionType
                when QuestionType.SingleSelection   then (q) -> new SingleSelectionQuestion(q)
                when QuestionType.MultipleSelection then (q) -> new MultipleSelectionQuestion(q)
                when QuestionType.Numeric           then (q) -> new NumericQuestion(q)
                when QuestionType.Text              then (q) -> new TextQuestion(q)
                else throw "unexpected question type: #{questionType}"

    class SelectionQuestion extends Question
        constructor: (q) ->
            super(q)
            questionOptionsArray = (q?.questionOptions ? []).map (o) -> new QuestionOption(o)
            @newQuestionOption = ko.observable()
            @questionOptions = ko.computed =>
                if @newQuestionOption()
                    questionOptionsArray.push(new QuestionOption(description: @newQuestionOption()))
                    @newQuestionOption(null)
                questionOptionsArray = questionOptionsArray.filter (qo) -> qo.valid()
            @valid = ko.computed(@validate)
        validate: =>
            @description() and @questionOptions().length > 0

    class SingleSelectionQuestion extends SelectionQuestion
        questionType: QuestionType.SingleSelection

    class MultipleSelectionQuestion extends SelectionQuestion
        questionType: QuestionType.MultipleSelection

    class NumericQuestion extends Question
        questionType: QuestionType.Numeric
        constructor: (q) ->
            super(q)
            @minValue = ko.observable(q?.minValue)
            @maxValue = ko.observable(q?.maxValue)
            @valid = ko.computed(@validate)
        validate: =>
            @description() #and...

    class TextQuestion extends Question
        questionType: QuestionType.Text
        constructor: (q) ->
            super(q)
            @valid = ko.computed(@validate)
        validate: => @description()

    class Enquete
        constructor: (e) ->
            @title = ko.observable(e?.title)
            @description = ko.observable(e?.description)
            @answerLink = ko.observable(e?.answerLink)
            @questions = ko.observableArray((e?.questions ? []).map (q) -> Question.create(q))
            @valid = ko.computed(@validate)
        validate: =>
            @title() and @questions().every (q) -> q.valid()
        addQuestion: (questionType) =>
            () => @questions.push(Question.constructorOf(questionType)())
        removeQuestion: (q) =>
            @questions.remove(q)

    class App
        constructor: () ->
            @enquete = ko.observable()
            @status = ko.observable(null)
            ko.computed => if @enquete() then @status(null)
            @canSave = ko.computed => @enquete()?.valid()
            @canUpdate = ko.computed => @enquete()?.valid()
        blank: () =>
            @enquete(new Enquete())
        load: (adminKey) =>
            Enquetes.findByAdminKey(adminKey).done (enquete) =>
                @enquete(enquete)
        save: =>
            Enquetes.create(@enquete()).done (d) =>
                location.href = "/admin##{d.adminKey}"
        update: =>
            adminKey = location.hash.substring(1)
            Enquetes.update(adminKey, @enquete()).done =>
                @load(adminKey).done =>
                    @status(200)

    class Controller
        route: ->
            switch location.pathname
                when '/'
                    app = new App()
                    app.blank()
                    @register(app)
                when '/admin'
                    app = new App()
                    app.load(location.hash.substring(1))
                    @register(app)
        register: (app) ->
            ko.applyBindings(app)
            $(document).ajaxStart -> $('.loading').show()
            $(document).ajaxStop  -> $('.loading').fadeOut()
            $(document).ajaxError (e, xhr) -> app.status(xhr.status)
            $('.notification').click -> $(@).fadeOut()

    new Controller().route()
