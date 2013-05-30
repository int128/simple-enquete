$ ->
    class Enquetes
        @findByAdminKey: (adminKey) ->
            $.ajax
                url: "/admin/#{adminKey}"
                type: 'get'
                dataType: 'json'
        @create: (enquete) ->
            $.ajax
                url: '/'
                type: 'post'
                contentType: 'application/json; Charset=UTF-8'
                data: ko.toJSON(enquete)
                dataType: 'json'
        @update: (adminKey, enquete) ->
            $.ajax
                url: "/admin/#{adminKey}"
                type: 'post'
                contentType: 'application/json; Charset=UTF-8'
                data: ko.toJSON(enquete)
                dataType: 'json'

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
        @create: (questionType, q) -> switch questionType
            when QuestionType.SingleSelection then new SingleSelectionQuestion(q)
            when QuestionType.MultipleSelection then new MultipleSelectionQuestion(q)
            when QuestionType.Numeric then new NumericQuestion(q)
            when QuestionType.Text then new TextQuestion(q)

    class SelectionQuestion extends Question
        constructor: (q) ->
            super(q)
            @newQuestionOption = ko.observable()
            @_questionOptions = (q?.questionOptions ? []).map (o) -> new QuestionOption(o)
            @questionOptions = ko.computed(@rearrangeQuestionOptions)
            @valid = ko.computed(@validate)
        validate: =>
            @description() and @questionOptions().length > 0
        rearrangeQuestionOptions: =>
            if @newQuestionOption()
                @_questionOptions.push(new QuestionOption(description: @newQuestionOption()))
                @newQuestionOption(null)
            @_questionOptions = @_questionOptions.filter (qo) -> qo.valid()

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
            @questions = ko.observableArray((e?.questions ? []).map (q) -> Question.create(QuestionType[q.questionType], q))
            @valid = ko.computed(@validate)
        validate: =>
            @title() and @questions().every (q) -> q.valid()
        addQuestion: (questionType) =>
            () => @questions.push(Question.create(questionType))
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
            Enquetes.findByAdminKey(adminKey).done (d) =>
                @enquete(new Enquete(d.enquete))
        save: =>
            Enquetes.create(@enquete()).done (d) =>
                location.href = "/admin##{d.adminKey}"
        update: =>
            adminKey = location.hash.substring(1)
            Enquetes.update(adminKey, @enquete()).done (d) =>
                @enquete(new Enquete(d.enquete))
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
