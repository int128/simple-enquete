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
            @id = qo.id if qo
            @description = ko.observable(qo.description if qo)
            @valid = ko.computed(@validate)
        validate: =>
            Boolean(@description())

    class Question
        constructor: (q) ->
            @id = q.id if q
            @description = ko.observable(q.description if q)
            @questionType = ko.observable(q.questionType if q)
            @newQuestionOption = ko.observable()
            @_questionOptions = if q and q.questionOptions then q.questionOptions.map (o) -> new QuestionOption(o) else []
            @questionOptions = ko.computed(@rearrangeQuestionOptions)
            @valid = ko.computed(@validate)
        validate: =>
            @description() and @questionOptions().length > 0
        rearrangeQuestionOptions: =>
            if @newQuestionOption()
                @_questionOptions.push(new QuestionOption(description: @newQuestionOption()))
                @newQuestionOption(null)
            @_questionOptions = @_questionOptions.filter (qo) -> qo.valid()

    class Enquete
        constructor: (e) ->
            @title = ko.observable(e.title if e)
            @description = ko.observable(e.description if e)
            @answerLink = ko.observable(e.answerLink if e)
            @questions = ko.observableArray(if e then e.questions.map (q) -> new Question(q) else [])
            @valid = ko.computed(@validate)
        validate: =>
            @title() and @questions().every (q) -> q.valid()
        addQuestion: (questionType) =>
            () => @questions.push(new Question(questionType: questionType))
        removeQuestion: (q) =>
            @questions.remove(q)

    class App
        constructor: () ->
            @enquete = ko.observable()
            @status = ko.observable(null)
            ko.computed => if @enquete() then @status(null)
            @canSave = ko.computed => @enquete().valid() if @enquete()
            @canUpdate = ko.computed => @enquete().valid() if @enquete()
        blank: () =>
            @enquete(new Enquete())
        load: (adminKey) =>
            Enquetes.findByAdminKey(adminKey).done (d) =>
                @enquete(new Enquete(d.enquete))
        save: =>
            Enquetes.create(app.enquete()).done (d) =>
                location.href = "/admin##{d.adminKey}"
        update: =>
            adminKey = location.hash.substring(1)
            Enquetes.update(adminKey, @enquete()).done (d) =>
                @enquete(new Enquete(d.enquete))
                @status(200)

    class Router
        route: =>
            switch location.pathname
                when '/'
                    app = new App()
                    app.blank()
                    ko.applyBindings(app)
                when '/admin'
                    app = new App()
                    app.load(location.hash.substring(1))
                    ko.applyBindings(app)

    new Router().route()

    $(document).ajaxStart -> $('.loading').show()
    $(document).ajaxStop  -> $('.loading').fadeOut()
    $(document).ajaxError (e, xhr) -> app.status(xhr.status)
    $('.notification').click -> $(@).fadeOut()
